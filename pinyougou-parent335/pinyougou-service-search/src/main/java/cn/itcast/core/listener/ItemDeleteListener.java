package cn.itcast.core.listener;

import cn.itcast.core.service.search.ItemSearchService;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class ItemDeleteListener implements MessageListener {

    @Resource
    ItemSearchService itemSearchService;
    @Override
    public void onMessage(Message message) {

        try {
            ActiveMQTextMessage activeMQTextMessage =  (ActiveMQTextMessage) message;
            String id = activeMQTextMessage.getText();
            System.out.println("删除索引库："+id);
            itemSearchService.deleteItemFromSolr(Long.parseLong(id));

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
