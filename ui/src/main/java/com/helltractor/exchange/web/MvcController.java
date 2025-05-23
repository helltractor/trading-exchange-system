package com.helltractor.exchange.web;

import com.helltractor.exchange.ApiException;
import com.helltractor.exchange.bean.AuthToken;
import com.helltractor.exchange.bean.TransferRequestBean;
import com.helltractor.exchange.client.RestClient;
import com.helltractor.exchange.ctx.UserContext;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.enums.UserType;
import com.helltractor.exchange.model.ui.UserProfileEntity;
import com.helltractor.exchange.support.LoggerSupport;
import com.helltractor.exchange.user.UserService;
import com.helltractor.exchange.util.HashUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Controller
public class MvcController extends LoggerSupport {

    public final static Pattern EMAIL = Pattern.compile("^[a-z0-9\\-\\.]+\\@([a-z0-9\\-]+\\.){1,3}[a-z]{2,20}$");

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestClient tradingApiClient;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() {
        // init users: user0@example.com ~ user99@example.com
        if (isLocalDevEnv()) {
            for (int i = 0; i <= 99; i++) {
                String email = "user" + i + "@example.com";
                String name = "User-" + i;
                String password = "password" + i;
                if (userService.fetchUserProfileByEmail(email) == null) {
                    logger.info("auto create user {} for local dev env...", email);
                    doSignup(email, name, password);
                }
            }
        }
    }

    @GetMapping("/")
    public ModelAndView index() {
        if (UserContext.getUserId() == null) {
            return redirect("/signin");
        }
        return prepareModelAndView("index");
    }

    @GetMapping("/signup")
    public ModelAndView signup() {
        if (UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelAndView("signup");
    }

    @PostMapping("/signup")
    public ModelAndView signup(@RequestParam("email") String email, @RequestParam("name") String name, @RequestParam("password") String password) {
        // check email
        if (email == null || email.isBlank()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid email."));
        }
        email = email.strip().toLowerCase();
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid email."));
        }
        if (userService.fetchUserProfileByEmail(email) != null) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Email exists."));
        }
        // check name
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid name."));
        }
        name = name.strip();
        // check password
        if (password == null || password.length() < 8 || password.length() > 32) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid password."));
        }
        doSignup(email, name, password);
        return redirect("/signin");
    }

    @PostMapping(value = "/websocket/token", produces = "application/json")
    @ResponseBody
    String requestWebSocketToken() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 无登录信息，返回JSON空字符串""
            return "\"\"";
        }
        // expire in 60 seconds
        AuthToken token = new AuthToken(userId, System.currentTimeMillis() + 60_000);
        String strToken = token.toSecureString(hmacKey);
        // 返回JSON字符串"xxx"
        return "\"" + strToken + "\"";
    }

    @GetMapping("/signin")
    public ModelAndView signin(HttpServletRequest request) {
        if (UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelAndView("signin");
    }

    @PostMapping("/signin")
    public ModelAndView signin(@RequestParam("email") String email, @RequestParam("password") String password, HttpServletRequest request, HttpServletResponse response) {
        // check email and password
        if (email == null || email.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        }
        if (password == null || password.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        }
        email = email.toLowerCase();
        try {
            // 获取用户配置
            UserProfileEntity userProfile = userService.signin(email, password);
            // 登录成功后设置Cookie
            AuthToken token = new AuthToken(userProfile.userId,
                    System.currentTimeMillis() + 1000 * cookieService.getExpiresInSeconds());
            cookieService.setSessionCookie(request, response, token);
        } catch (ApiException e) {
            // 登录失败
            logger.warn("sign in failed for {}", e.getMessage(), e);
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        } catch (Exception e) {
            // 登录失败
            logger.warn("sign in failed for {}", e.getMessage(), e);
            return prepareModelAndView("signin", Map.of("email", email, "error", "Internal server error."));
        }
        logger.info("signin ok.");
        return redirect("/");
    }

    @GetMapping("/signout")
    public ModelAndView signout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.deleteSessionCookie(request, response);
        return redirect("/");
    }

    private UserProfileEntity doSignup(String email, String name, String password) {
        // 注册用户
        UserProfileEntity profile = userService.signup(email, name, password);
        // 本地开发环境下自动给用户增加资产
        if (isLocalDevEnv()) {
            logger.warn("auto deposit assets for user {} in local dev env...", profile.email);
            Random random = new Random(profile.userId);
            deposit(profile.userId, AssetEnum.BTC, new BigDecimal(random.nextInt(5_00, 10_00)).movePointLeft(2));
            deposit(profile.userId, AssetEnum.USD,
                    new BigDecimal(random.nextInt(100000_00, 400000_00)).movePointLeft(2));
        }
        logger.info("user signed up: {}", profile);
        return profile;
    }

    private boolean isLocalDevEnv() {
        logger.info("activeProfiles: {}, defaultProfiles: {}", environment.getActiveProfiles(), environment.getDefaultProfiles());
        return environment.getActiveProfiles().length == 0
                && Arrays.equals(environment.getDefaultProfiles(), new String[]{"default"});
    }

    private void deposit(Long userId, AssetEnum asset, BigDecimal amount) {
        var req = new TransferRequestBean();
        req.transferId = HashUtil.sha256(userId + "/" + asset + "/" + amount.stripTrailingZeros().toPlainString())
                .substring(0, 32);
        req.amount = amount;
        req.asset = asset;
        req.fromUserId = UserType.DEBT.getInternalUserId();
        req.toUserId = userId;
        tradingApiClient.post(Map.class, "/internal/transfer", null, req);
    }

    private ModelAndView prepareModelAndView(String view, String key, Object value) {
        ModelAndView mv = new ModelAndView(view);
        mv.addObject(key, value);
        addGlobalModel(mv);
        return mv;
    }

    private ModelAndView prepareModelAndView(String view, Map<String, Object> model) {
        ModelAndView mv = new ModelAndView(view);
        mv.addAllObjects(model);
        addGlobalModel(mv);
        return mv;
    }

    private ModelAndView prepareModelAndView(String view) {
        ModelAndView mv = new ModelAndView(view);
        addGlobalModel(mv);
        return mv;
    }

    private ModelAndView notFound() {
        ModelAndView mv = new ModelAndView("404");
        addGlobalModel(mv);
        return mv;
    }

    private void addGlobalModel(ModelAndView mv) {
        final Long userId = UserContext.getUserId();
        mv.addObject("__userId__", userId);
        mv.addObject("__profile__", userId == null ? null : userService.getUserProfile(userId));
        mv.addObject("__time__", Long.valueOf(System.currentTimeMillis()));
    }

    private ModelAndView redirect(String url) {
        return new ModelAndView("redirect:" + url);
    }
}
