package cn.itcast.core.service.pay;

import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.utils.http.HttpClient;
import cn.itcast.core.utils.uniquekey.IdWorker;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;
import java.util.Map;

@Service
public class WxPayServiceImpl implements WxPayService {


    //注入微信支付凭证
    @Value("${appid}")
    private String appid;
    //商户号
    @Value("${partner}")
    private String partner;


    //秘钥
    @Value("${partnerkey}")
    private String partnerkey;

    //注入idWorker
    @Autowired
    private IdWorker idWorker;


    //注入redis模板对象
    @Autowired
    private RedisTemplate redisTemplate;

    //注入订单日志接口代理对象
    @Autowired
    private PayLogDao payLogDao;



    @Override
    public Map createNative(String userId) {
        Map<String,String> maps = null;
        try {


            //从redis中获取订单日志对象
            PayLog payLog = (PayLog) redisTemplate.boundHashOps("payLog").get(userId);

            // 1，创建map对象，封装向微信平台发送参数（注意：参数格式为xml,请求方式POST）

            //创建map对象，封装数据
            maps = new HashedMap();

            //appid
            maps.put("appid",appid);
            //商户号
            maps.put("mch_id",partner);

            //随机字符串
            maps.put("nonce_str", WXPayUtil.generateNonceStr());

            //签名，使用微信工具类提供算法生成一个带有签名的xml格式参数

            //描述
            maps.put("body","品优购订单支付");


            //out_trade_no 交易订单号
            //long out_trade_no = idWorker.nextId();
            maps.put("out_trade_no",payLog.getOutTradeNo());

            //交易金额
            maps.put("total_fee",payLog.getTotalFee()+"");

            //支付机器ip
            maps.put("spbill_create_ip","127.0.0.1");

            //回调地址
            maps.put("notify_url","http://www.itcast.cn");

            //交易类型
            maps.put("trade_type","NATIVE");

           //2，把map对象转换为具有签名的xml格式参数
            //生成xml格式参数
            //生成一个具有签名，xml格式参数
            String xmlParam = WXPayUtil.generateSignedXml(maps, partnerkey);

            //3，创建httpClient 调用 微信支付平台接口，发送请求，传递xml参数
            //向微信支付平台统一下单
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //设置请求方式
            //https
            httpClient.setHttps(true);
            //设置参数
            httpClient.setXmlParam(xmlParam);
            httpClient.post();

            //4，获取返回值（注意：返回值也是xml）
            String xmlResult = httpClient.getContent();

            //5, 把返回值转换为map对象
            Map<String, String> result = WXPayUtil.xmlToMap(xmlResult);


           // 6，封装支付页面需要的数据： 支付订单号，金额，支付链接地址
            result.put("total_fee",payLog.getTotalFee()+"");
            result.put("out_trade_no",payLog.getOutTradeNo());

            return  result;


        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

    /**
     * 需求：根据订单号（交易流水号）查询订单支付状态 （支付成功，失败，关闭，未支付等等）
     * 参数：String out_trade_no
     * 返回值：map
     *
     * 业务流程：
     * 1，创建map对象，封装向微信平台发送参数（注意：参数格式为xml,请求方式POST）
     * 2，把map对象转换为具有签名的xml格式参数
     * 3，创建httpClient 调用 微信支付平台接口，发送请求，传递xml参数
     * 4，获取返回值（注意：返回值也是xml）
     * 5, 把返回值转换为map对象
     *
     *  */
    public Map queryPayStatus(String out_trade_no) {

        try {
            // 1，创建map对象，封装向微信平台发送参数（注意：参数格式为xml,请求方式POST）
            //创建map对象，封装数据
            Map maps = new HashedMap();

            //appid
            maps.put("appid",appid);
            //商户号
            maps.put("mch_id",partner);

            //随机字符串
            maps.put("nonce_str", WXPayUtil.generateNonceStr());

            //支付订单号
            maps.put("out_trade_no",out_trade_no);

            //2，把map对象转换为具有签名的xml格式参数
            //生成xml格式参数
            //生成一个具有签名，xml格式参数
            String xmlParam = WXPayUtil.generateSignedXml(maps, partnerkey);

            //3，创建httpClient 调用 微信支付平台接口，发送请求，传递xml参数
            //向微信支付平台统一下单
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            //设置请求方式
            //https
            httpClient.setHttps(true);
            //设置参数
            httpClient.setXmlParam(xmlParam);
            httpClient.post();

            //4，获取返回值（注意：返回值也是xml）

            String resultxml = httpClient.getContent();

            //5, 把返回值转换为map对象
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultxml);


            //判断，如果支付成功，更新订单交易日志
            if(resultMap.get("trade_state").equals("SUCCESS")){
                //创建订单对象
                PayLog payLog = new PayLog();
                payLog.setOutTradeNo(out_trade_no);
                payLog.setPayTime(new Date());
                payLog.setTradeState("1");
                payLog.setTransactionId(resultMap.get("transaction_id"));

                //更新
                payLogDao.updateByPrimaryKeySelective(payLog);


            }

            return  resultMap;




        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
