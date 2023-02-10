package test.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import test.util.DingdingUtils;
import test.util.HttpUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @Value("${chatgpt.url}")
    private String url;
    @Value("${chatgpt.key1}")
    private String key1;

    @Value("${chatgpt.key2}")
    private String key2;

    public static Integer count = 1;

    @Autowired
    private DingdingUtils dingdingUtils;


    @PostMapping("test")
    public void test(@RequestBody String body, HttpServletRequest request,
                     @RequestParam(required = false)String robotCode) throws Exception {
        System.out.println("请求头 ： "+JSON.toJSONString(request.getHeaderNames()));
        System.out.println("请求体 ： "+body);
        System.out.println("请求体2 ： "+JSON.toJSONString(request.getParameterMap()));

        JSONObject jsonObject = JSONObject.parseObject(body);
        String text = jsonObject.getJSONObject("text").getString("content");

        Map<String,String> headMap = new HashMap<>();
        String key = key1;
        if(count%2 == 0){
            key = key2;
            count++;
        }

        headMap.put("Authorization","Bearer "+key);
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("model","text-davinci-003");
        bodyMap.put("prompt",text);
        bodyMap.put("temperature",0);
        bodyMap.put("max_tokens",1000);
        System.out.println("请求chatGPT各参数 url = "+url+" , body ="+bodyMap+" , head = "+headMap);

        String s = HttpUtil.doPostWithJsonAndHeader(url, JSON.toJSONString(bodyMap), headMap);
        System.out.println("请求chatGPT结果 "+s);
        String rs = "网络故障，请稍后再试";
         if(!StringUtils.isEmpty(s) && JSONObject.parseObject(s).getJSONArray("choices")!=null
                 && JSONObject.parseObject(s).getJSONArray("choices").getJSONObject(0)!=null
                 && JSONObject.parseObject(s).getJSONArray("choices").getJSONObject(0).getString("text")!=null){
             jsonObject = JSONObject.parseObject(s);
             rs = jsonObject.getJSONArray("choices").getJSONObject(0).getString("text");
         }
        dingdingUtils.sendgroupChatMsg(rs,robotCode);
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
