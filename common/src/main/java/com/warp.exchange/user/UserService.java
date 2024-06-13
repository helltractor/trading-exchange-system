package com.warp.exchange.user;

import com.warp.exchange.api.ApiException;
import com.warp.exchange.entity.ui.PasswordAuthEntity;
import com.warp.exchange.entity.ui.UserEntity;
import com.warp.exchange.entity.ui.UserProfileEntity;
import com.warp.exchange.enums.ApiError;
import com.warp.exchange.enums.UserType;
import com.warp.exchange.support.AbstractDbService;
import com.warp.exchange.util.HashUtil;
import com.warp.exchange.util.RandomUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserService extends AbstractDbService {
    
    public UserProfileEntity getUserProfile(String userId) {
        return dataBase.get(UserProfileEntity.class, userId);
    }
    
    /**
     * 通过邮件从数据库查找用户信息
     *
     * @param email
     * @return
     */
    @Nullable
    public UserProfileEntity fetchUserProfileByEmail(String email) {
        return dataBase.from(UserProfileEntity.class).where("email = ?", email).first();
    }
    
    /**
     * 通过邮件查找用户信息
     *
     * @param email
     * @return
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
     *
     * @param email
     * @param name
     * @param password
     * @return
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
     *
     * @param email
     * @param passwd
     * @return
     */
    public UserProfileEntity signin(String email, String passwd) {
        UserProfileEntity userProfile = getUserProfileByEmail(email);
        // 按用户ID查找密码身份验证
        PasswordAuthEntity pa = dataBase.fetch(PasswordAuthEntity.class, userProfile.userId);
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
