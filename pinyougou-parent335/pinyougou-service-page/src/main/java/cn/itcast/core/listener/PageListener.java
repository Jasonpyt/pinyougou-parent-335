package cn.itcast.core.listener;

import cn.itcast.core.service.staticpage.StaticPageService;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * 自定义监听器：生成商品详情的静态页
 */



public class PageListener implements MessageListener{

    @Resource
    private StaticPageService staticPageService;

    /**
     * 获取消息并且消费消息
     * @param message
     */
    @Override
    public void onMessage(Message message) {

        try {
            // 取出消息
            ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage) message;
            String id = activeMQTextMessage.getText();
            // 消费消息
            System.out.println("service-page获取的id："+id);
            staticPageService.getHtml(Long.parseLong(id));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
