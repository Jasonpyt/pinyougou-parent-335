package cn.itcast.core.controller.pay;

import cn.itcast.core.entity.Result;
import cn.itcast.core.service.pay.WxPayService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pay")
public class WxPayController {


    //注入服务层对象
    @Reference
    private WxPayService wxPayService;


    /**
     * 需求：提交订单，跳转支付页面，生成二维码 （需要向后台发送请求，获取二维码支付地址）
     * 请求：pay/createNative.do
     * 参数：无
     * 返回值：Map (支付地址。支付金额，支付是否成功)
     */
    @RequestMapping("createNative")
    public Map createNative(){
        //1,获取用户登录名
        String userID = SecurityContextHolder.getContext().getAuthentication().getName();
        //2,远程调用服务层方法
        Map map = wxPayService.createNative(userID);

        return map;
    }

    /**
     * 需求：根据订单号（交易流水号）查询订单支付状态 （支付成功，失败，关闭，未支付等等）
     * 请求：'pay/queryPayStatus.do?out_trade_no='+out_trade_no
     * 参数：String out_trade_no
     * 返回值：Result
     *
     * 业务流程：
     * 1,判断查询结果是否存在，如果不存在，支付错误
     * 2，否则存在，判断支付是否成功，如果成功，返回true
     * 3,如果超时，返回超时
     *
     * 查询注意事项：
     * 没过3s检查一下二维码状态
     * 查询总时长为5分钟
     *
     *  */
    @RequestMapping("queryPayStatus")
    public Result queryPayStatus(String out_trade_no){
        int i = 0;
        while (true){

            //调用服务对象，查询二维码状态
            Map map = wxPayService.queryPayStatus(out_trade_no);

            System.out.println(map.toString());

            //判断map是否存在
            if(map==null){
                return  new Result(false,"支付失败");
            }

            if(map.get("trade_state").equals("SUCCESS")){

                //更新支付日志

                return new Result(true,"支付成功");
            }

            try {
                //每隔3s查询一次
                Thread.sleep(3000);

            } catch (Exception e) {
                e.printStackTrace();
            }

            i++;

            if(i>100){
                return new Result(false,"二维码超时");
            }


        }







    }
}
