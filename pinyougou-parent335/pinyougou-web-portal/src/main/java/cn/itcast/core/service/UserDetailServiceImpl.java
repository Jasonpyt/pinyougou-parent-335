package cn.itcast.core.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;

/**
 * @Auther:Jerry Lau
 * @Date: 2019/3/7 0007
 * @Description: cn.itcast.core.service
 * @Version: 1.0
 */
public class UserDetailServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HashSet<GrantedAuthority> authorities = new HashSet<>();

        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority("ROLE_USER");
        //设置权限
        authorities.add(simpleGrantedAuthority);
        //将权限添加到用户对象中
        User user = new User(username, "",authorities);

        return user;
    }
}
