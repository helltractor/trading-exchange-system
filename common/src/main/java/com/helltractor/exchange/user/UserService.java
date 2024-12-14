package com.helltractor.exchange.user;

import com.helltractor.exchange.ApiException;
import com.helltractor.exchange.model.ui.PasswordAuthEntity;
import com.helltractor.exchange.model.ui.UserEntity;
import com.helltractor.exchange.model.ui.UserProfileEntity;
import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.enums.UserType;
import com.helltractor.exchange.support.AbstractDbService;
import com.helltractor.exchange.util.HashUtil;
import com.helltractor.exchange.util.RandomUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserService extends AbstractDbService {
    
    public UserProfileEntity getUserProfile(Long userId) {
        return dataBase.get(UserProfileEntity.class, userId);
    }
    
    /**
     * 通过邮件从数据库查找用户信息
     */
    @Nullable
    public UserProfileEntity fetchUserProfileByEmail(String email) {
        return dataBase.from(UserProfileEntity.class).where("email = ?", email).first();
    }
    
    /**
     * 通过邮件查找用户信息
     */
    public UserProfileEntity getUserProfileByEmail(String email) {
        UserProfileEntity userProfile = fetchUserProfileByEmail(email);
        if (userProfile == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }
    
    /**
     * 通过邮箱，名字和密码注册用户
     */
    public UserProfileEntity signup(String email, String name, String password) {
        final long timestamp = System.currentTimeMillis();
        // 插入用户信息
        var user = new UserEntity();
        user.type = UserType.TRADER;
        user.createTime = timestamp;
        dataBase.insert(user);
        // 插入用户配置
        var up = new UserProfileEntity();
        up.userId = user.id;
        up.email = email;
        up.name = name;
        up.createTime = up.updateTime = timestamp;
        dataBase.insert(up);
        // 插入密码身份验证
        var pa = new PasswordAuthEntity();
        pa.userId = user.id;
        pa.random = RandomUtil.createRandomString(32);
        pa.password = HashUtil.hmacSha256(password, pa.random);
        dataBase.insert(pa);
        return up;
    }
    
    /**
     * 使用邮箱，密码进行登录
     */
    public UserProfileEntity signin(String email, String passwd) {
        UserProfileEntity userProfile = getUserProfileByEmail(email);
        // 按用户ID查找密码身份验证
        PasswordAuthEntity pa = dataBase.fetch(PasswordAuthEntity.class, userProfile.userId);
        logger.info("pa: {}", pa);
        if (pa == null) {
            throw new ApiException(ApiError.USER_CANNOT_SIGNIN);
        }
        // 检查密码哈希
        String hash = HashUtil.hmacSha256(passwd, pa.random);
        if (!hash.equals(pa.password)) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }
}
