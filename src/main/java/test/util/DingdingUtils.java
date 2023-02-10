package test.util;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DingdingUtils {

    @Value("${dingding.url.robotSendUrl}")
    private String groupChatRobotSendUrl;

    @Value("${dingding.url.defRobotCode}")
    private String defRobotCode;

    public void sendgroupChatMsg(String content,String robotCode) throws Exception {
        try {
            String code = defRobotCode;
            if(!StringUtils.isEmpty(robotCode)){
                code = robotCode;
            }
            DingTalkClient client = new DefaultDingTalkClient(groupChatRobotSendUrl+code);
            OapiRobotSendRequest request = new OapiRobotSendRequest();

            request.setMsgtype("markdown");
            OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
            markdown.setTitle("chatGPT提醒");
            markdown.setText(content);
            request.setMarkdown(markdown);

            OapiRobotSendResponse execute = client.execute(request);
            if(!"0".equals(execute.getErrorCode()) || !"ok".equals(execute.getErrmsg())){
                throw new Exception("调用钉钉接口失败 错误码："+execute.getErrcode()+" 错误信息："+execute.getErrmsg());
            }
        }catch (Exception e){
            System.out.println("调用钉钉发送群聊消息失败 -> "+e);
            throw e;
        }

    }

}
