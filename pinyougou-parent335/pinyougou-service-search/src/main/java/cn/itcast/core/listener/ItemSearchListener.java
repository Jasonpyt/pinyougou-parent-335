package cn.itcast.core.listener;

import cn.itcast.core.service.search.ItemSearchService;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * 自定义的消息监听器：完成商品上架
 */
public class ItemSearchListener implements MessageListener{

    @Resource
    private ItemSearchService itemSearchService;

    /**
     * 获取消息并且处理消息
     * @param message
     */

    @Override
    public void onMessage(Message message) {

        try {
            // 取出消息
            ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage) message;
            String id = activeMQTextMessage.getText();
            // 消费消息
            System.out.println("service-search获取的id："+id);
            itemSearchService.isShow(Long.parseLong(id));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
