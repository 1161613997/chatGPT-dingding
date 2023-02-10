package test.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HttpUtil {
    private static PoolingHttpClientConnectionManager connMgr;

    private static RequestConfig requestConfig;

    private static int MAX_TIMEOUT;

    @Value("${HttpUtil.max_timeout:1000000}")
    public void setMaxTimeout(int data) {
        HttpUtil.MAX_TIMEOUT = data;
    }


    private static class HttpUtilHolder {
        private static final HttpUtil INSTANCE = new HttpUtil();
    }

    public static HttpUtil getInstance() {
        return HttpUtilHolder.INSTANCE;
    }

    static {

        // 设置连接池
        connMgr = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connMgr.setMaxTotal(100);
        connMgr.setDefaultMaxPerRoute(connMgr.getMaxTotal());
        connMgr.setValidateAfterInactivity(1000);

        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时
        configBuilder.setConnectTimeout(MAX_TIMEOUT);
        // 设置读取超时
        configBuilder.setSocketTimeout(MAX_TIMEOUT);
        // 设置从连接池获取连接实例的超时
        configBuilder.setConnectionRequestTimeout(MAX_TIMEOUT);
        requestConfig = configBuilder.build();

    }

    public static String doGet(String url) throws Exception {
        return doGet(url, null, null);
    }

    public static String doGet(String url, Map<String, String> params) throws Exception {
        return doGet(url, params, null);
    }

    public static String doGet(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
        //1.创建http客户端对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String result = "";
        try {
            //2.构建一个URI对象,根据url及参数
            URIBuilder uriBuilder = new URIBuilder(url);
            //2.1请求参数
            if (params != null && params.size() > 0) {
                for (String key : params.keySet()) {
                    uriBuilder.addParameter(key, params.get(key));
                }
            }
            URI uri = uriBuilder.build();
            //3.创建一个get请求
            HttpGet httpGet = new HttpGet(uri);
            //3.1请求头
            if (headers != null && headers.size() > 0) {
                for (String key : headers.keySet()) {
                    httpGet.addHeader(key, headers.get(key));
                }
            }
            //4.执行get请求,得到响应
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //5.从响应对象中获取响应数据
                result = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }

        return result;
    }

    public static String doPost(String url) throws Exception {
        return doPost(url, null);
    }

    public static String doPost(String url, Map<String, String> params) throws Exception {
        String result = "";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        //封装post请求参数
        int size = params.size();
        if (params != null && size > 0) {
            List<NameValuePair> pairList = new ArrayList<>(size);
            for (String key : params.keySet()) {
                pairList.add(new BasicNameValuePair(key, params.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8"));
            httpPost.setEntity(formEntity);
            //执行post请求
            CloseableHttpResponse response = null;
            try {
                httpPost.setConfig(requestConfig);
                response = httpClient.execute(httpPost);
                result = EntityUtils.toString(response.getEntity(), "UTF-8");
            } catch (IOException e) {
                throw e;
            } finally {
                HttpClientUtils.closeQuietly(response);
                HttpClientUtils.closeQuietly(httpClient);
            }
        }

        return result;
    }


    /**
     * 发送 POST 请求（HTTP），JSON形式
     *    
     */
    public static String doPostWithJsonAndHeader(String url, String jsonParams,Map<String,String> headers) throws Exception {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String result = null;
        HttpPost httpPost = new HttpPost(url);

        try {

            httpPost.setConfig(requestConfig);
            StringEntity stringEntity = new StringEntity(jsonParams, "UTF-8");// 解决中文乱码问题
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            httpPost.setEntity(stringEntity);
            for (Map.Entry<String,String> entry:headers.entrySet()){
                httpPost.setHeader(entry.getKey(),entry.getValue());
            }
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (IOException e) {
            throw e;
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
        return result;

    }





    /**
     * 发送 SSL POST 请求（HTTPS），K-V形式
     *
     * @param    
     */
    public static String doPostSSL(String url, Map<String, String> params) {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(createSSLConnSocketFactory())
                .setConnectionManager(connMgr)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpPost httpPost = new HttpPost(url);

        CloseableHttpResponse response = null;

        String result = null;

        try {

            httpPost.setConfig(requestConfig);

            List pairList = new ArrayList(params.size());

            for (Map.Entry<String, String> entry : params.entrySet()) {

                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue().toString());

                pairList.add(pair);

            }

            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));

            response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {

                return null;

            }

            HttpEntity entity = response.getEntity();

            if (entity != null) {

                result = EntityUtils.toString(entity, "UTF-8");

            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }

        return result;
    }

    /**
     *     * 发送 SSL POST 请求（HTTPS），JSON形式
     * <p>
     *    
     */
    public static String doPostSSL(String url, Object json) {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(createSSLConnSocketFactory())
                .setConnectionManager(connMgr)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;
        String result = null;

        try {

            httpPost.setConfig(requestConfig);

            StringEntity stringEntity = new StringEntity(json.toString(), "UTF-8");// 解决中文乱码问题
            stringEntity.setContentEncoding("UTF-8");

            stringEntity.setContentType("application/json");

            httpPost.setEntity(stringEntity);

            response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {

                return null;

            }

            HttpEntity entity = response.getEntity();

            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {

            e.printStackTrace();

        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);

        }

        return result;

    }

    /**
     *     * 创建SSL安全连接
     *    
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        return sslsf;

    }

    /**
     *     * 原生字符串发送put请求
     *     * @param url
     *     * @param token
     *     * @param jsonStr
     *     * @return
     **/
    public static String doPut(String url, String token, String jsonStr) {

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPut httpPut = new HttpPut(url);

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();

        httpPut.setConfig(requestConfig);

        httpPut.setHeader("Content-type", "application/json");

        httpPut.setHeader("DataEncoding", "UTF-8");

        httpPut.setHeader("access-token", token);

        CloseableHttpResponse response = null;

        try {

            httpPut.setEntity(new StringEntity(jsonStr));

            response = httpClient.execute(httpPut);

            HttpEntity entity = response.getEntity();

            String result = EntityUtils.toString(entity);

            return result;

        } catch (ClientProtocolException e) {

            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }

        return null;

    }

    /**
     *     * 发送delete请求
     *     * @param url
     *     * @param token
     *     * @param jsonStr
     *     * @return
     **/
    public static String doDelete(String url, String token, String jsonStr) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-type", "application/json");
        httpDelete.setHeader("DataEncoding", "UTF-8");
        httpDelete.setHeader("access-token", token);
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            return result;

        } catch (ClientProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
        return null;
    }
}
