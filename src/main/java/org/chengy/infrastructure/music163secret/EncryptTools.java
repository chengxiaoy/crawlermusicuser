package org.chengy.infrastructure.music163secret;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.chengy.core.HTTPConnectionManager;
import org.chengy.core.HttpHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Created by nali on 2017/9/14.
 */
public class EncryptTools {

    private static String encSecKey = "";

    //AES加密
    private static String encrypt(String text, String secKey) throws Exception {
        byte[] raw = secKey.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        // "算法/模式/补码方式"
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
        IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(text.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    //字符填充
    private static String zfill(String result, int n) {
        if (result.length() >= n) {
            result = result.substring(result.length() - n, result.length());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = n; i > result.length(); i--) {
                stringBuilder.append("0");
            }
            stringBuilder.append(result);
            result = stringBuilder.toString();
        }
        return result;
    }

    public static Pair<String, String> encryptCommonAPI(String text) throws Exception {

        //私钥，随机16位字符串（自己可改）
        String secKey = "cd859f54539b24b7";
        String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
        String nonce = "0CoJUm6Qyw8W8jud";
        String pubKey = "010001";
        //2次AES加密，得到params
        String params = EncryptTools.encrypt(EncryptTools.encrypt(text, nonce), secKey);
        if (encSecKey.length() > 0) {
            return new ImmutablePair<>(params, encSecKey);
        }

        StringBuffer stringBuffer = new StringBuffer(secKey);
        //逆置私钥
        secKey = stringBuffer.reverse().toString();
        String hex = Hex.encodeHexString(secKey.getBytes());
        BigInteger bigInteger1 = new BigInteger(hex, 16);
        BigInteger bigInteger2 = new BigInteger(pubKey, 16);
        BigInteger bigInteger3 = new BigInteger(modulus, 16);
        //RSA加密计算
        BigInteger bigInteger4 = bigInteger1.pow(bigInteger2.intValue()).remainder(bigInteger3);
        encSecKey = Hex.encodeHexString(bigInteger4.toByteArray());
        //字符填充
        encSecKey = EncryptTools.zfill(encSecKey, 256);

        return new ImmutablePair<>(params, encSecKey);

    }


    public static Document commentAPI(String text, String url) throws Exception {
        Pair<String, String> pair = encryptCommonAPI(text);
        String params = pair.getLeft();
        String encSecKey = pair.getRight();
        CloseableHttpClient httpClient = HttpHelper.client();

        HttpPost post = new HttpPost(url);
        post.addHeader("Referer", "http://music.163.com/");


        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("params", params));
        form.add(new BasicNameValuePair("encSecKey", encSecKey));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
        //entity.setContentType("application/x-www-form-urlencoded");
        post.setEntity(entity);

        String responseData = "";
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            responseData = EntityUtils.toString(response.getEntity());
        }
        Document document = Jsoup.parse(responseData);

        return document;
    }

    public static String fansUrl = "https://music.163.com/weapi/user/getfolloweds?csrf_token=";
    public static String loginUrl = "https://music.163.com/weapi/v1/resource/comments/R_SO_4_437859519/";
    public static String songUrl = "https://music.163.com/weapi/song/enhance/player/url?csrf_token=";
    public static String songParams = "{\"ids\": \"[31384819]\", \"br\": 128000, \"csrf_token\": \"\"}";
    String str = "{uid: \"533856\", offset: \"0\", total: \"true\", limit: \"20\", csrf_token: \"\"}";
    static String hehe = "{userId: \"533856\", offset: \"0\", total: \"true\", limit: \"20\", csrf_token: \"\"}";
    public static String fansParams = "{\"userId\": \"533856\", \"offset\": \"0\", \"total\": \"true\", \"limit\": \"20\", \"csrf_token\": \"\"}";
    public static String loginParams = "{\"usernassme\": \"\", \"rememberLogin\": \"true\", \"password\": \"\"}";


    public static void main(String[] args) throws Exception {

        Document document = commentAPI(Music163ApiCons.getFollowedParams("330313", 1, 20), Music163ApiCons.getFollowedUrl("330313"));
        System.out.println(document.text());
    }


}
