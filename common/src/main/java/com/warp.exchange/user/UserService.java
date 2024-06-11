package com.warp.exchange.user;

import com.warp.exchange.api.ApiException;
import com.warp.exchange.entity.ui.PasswordAuthEntity;
import com.warp.exchange.entity.ui.UserEntity;
import com.warp.exchange.entity.ui.UserProfileEntity;
import com.warp.exchange.enums.ApiError;
import com.warp.exchange.enums.UserType;
import com.warp.exchange.support.AbstractDbSupport;
import com.warp.exchange.util.HashUtil;
import com.warp.exchange.util.RandomUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserService extends AbstractDbSupport {
    
    public UserProfileEntity getUserProfile(String userId) {
        return dataBase.get(UserProfileEntity.class, userId);
    }
    
    @Nullable
    public UserProfileEntity fetchUserProfileByEmail(String email) {
        return dataBase.from(UserProfileEntity.class).where("email = ?", email).first();
    }
    
    /**
     * Get user profile by email.
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
     * Sign up with email, name and password.
     *
     * @param email
     * @param name
     * @param password
     * @return
     */
    public UserProfileEntity signup(String email, String name, String password) {
        final long timestamp = System.currentTimeMillis();
        // insert user
        var user = new UserEntity();
        user.type = UserType.TRADER;
        user.createTime = timestamp;
        dataBase.insert(user);
        // insert user profile:
        var up = new UserProfileEntity();
        up.userId = user.id;
        up.email = email;
        up.name = name;
        up.createTime = up.updateTime = timestamp;
        dataBase.insert(up);
        // insert password auth:
        var pa = new PasswordAuthEntity();
        pa.userId = user.id;
        pa.random = RandomUtil.createRandomString(32);
        pa.password = HashUtil.hmacSha256(password, pa.random);
        dataBase.insert(pa);
        return up;
    }
    
    /**
     * Sign in with email and password.
     *
     * @param email
     * @param passwd
     * @return
     */
    public UserProfileEntity signin(String email, String passwd) {
        UserProfileEntity userProfile = getUserProfileByEmail(email);
        // find PasswordAuthEntity by user id:
        PasswordAuthEntity pa = dataBase.fetch(PasswordAuthEntity.class, userProfile.userId);
        if (pa == null) {
            throw new ApiException(ApiError.USER_CANNOT_SIGNIN);
        }
        // check password hash:
        String hash = HashUtil.hmacSha256(passwd, pa.random);
        if (!hash.equals(pa.password)) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }
}
