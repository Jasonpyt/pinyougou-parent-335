package cn.itcast.core.service.user;

import cn.itcast.core.dao.user.UserDao;
import cn.itcast.core.pojo.user.User;
import cn.itcast.core.utils.md5.MD5Util;
import com.alibaba.dubbo.config.annotation.Service;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.annotation.Resource;
import javax.jms.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Destination smsDestination;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    UserDao userDao;
    /**
     * 用户获取短信验证码
     * @param phone
     */
    @Override
    public void sendCode(final String phone) {
        // 将获取短信验证码的数据发送到mq中
        // 手机号、验证码、签名、模板
        final String code = RandomStringUtils.randomNumeric(6);
        System.out.println("获取的验证码是:"+code);
        //保存用户注册信息到redis中
        redisTemplate.boundValueOps(phone).set(code);
        redisTemplate.boundValueOps("phone").expire(5, TimeUnit.MINUTES);

        jmsTemplate.send(smsDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                // 封装map消息体
                MapMessage mapMessage = session.createMapMessage();
                mapMessage.setString("phoneNumbers", phone);
                mapMessage.setString("signName", "阮文");
                mapMessage.setString("templateCode", "SMS_140720901");
                mapMessage.setString("templateParam", "{\"code\":\""+code+"\"}");

                return mapMessage;
            }
        });
    }

    /**
     * 用户注册
     * @param user
     * @param smscode
     */
    @Override
    public void add(User user, String smscode) {
        String code = (String) redisTemplate.boundValueOps(user.getPhone()).get();
        if (smscode != null && !"".equals(smscode) && smscode.equalsIgnoreCase(code)){
            user.setPassword(MD5Util.MD5Encode(user.getPassword(),null));
            user.setCreated(new Date());
            user.setUpdated(new Date());
            userDao.insertSelective(user);
       }else {
            throw new RuntimeException("注册失败,输入的验证码不正确");
        }
    }
}
