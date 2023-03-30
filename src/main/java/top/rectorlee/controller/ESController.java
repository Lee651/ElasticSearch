package top.rectorlee.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.rectorlee.domain.News;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lee
 * @description
 * @date 2023-03-18  14:47:45
 */
@RestController
@RequestMapping("/es")
public class ESController {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    // 标签模糊查询
    @RequestMapping("/tag")
    public List<String> getTags(String tag) throws IOException {
        Request request = new Request("GET", "news/_search");

        request.setJsonEntity(String.format("{" +
                "  \"_source\": false, \n" +
                "  \"suggest\": {\n" +
                "    \"tips\": {\n" +
                "     \"prefix\": \"%s\",\n" +
                "     \"completion\": {\n" +
                "       \"field\": \"tags\",\n" +
                "       \"size\": 10,\n" +
                "       \"skip_duplicates\": true\n" +
                "     }\n" +
                "    }\n" +
                "  }\n" +
                "}", tag));

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);

        String responseStr = EntityUtils.toString(response.getEntity());

        JSONObject jsonObject = JSONObject.parseObject(responseStr).getJSONObject("suggest").getJSONArray("tips").getJSONObject(0);

        JSONArray array = jsonObject.getJSONArray("options");

        List<String> list = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            Object obj = array.get(i);
            String text = JSONObject.parseObject(obj.toString()).getString("text");

            list.add(text);
        }

        return list;
    }

    // 高亮查询
    @RequestMapping("/highLightSearch")
    public List<News> getContent(String keyword) throws IOException {
        Request request = new Request("GET", "news/_search");

        request.setJsonEntity(String.format("{\n" +
                "  \"_source\": [\"id\", \"content\", \"title\", \"url\"], \n" +
                "  \"query\": {\n" +
                "    \"multi_match\": {\n" +
                "      \"query\": \"%s\",\n" +
                "      \"fields\": [\"title\", \"content\"]\n" +
                "    }\n" +
                "  },\n" +
                "  \"highlight\": {\n" +
                "    \"pre_tags\": \"<span class='hl'>\",\n" +
                "    \"post_tags\": \"</span>\",\n" +
                "    \"fields\": {\n" +
                "      \"title\": {},\n" +
                "      \"content\": {}\n" +
                "    }\n" +
                "  }\n" +
                "}\n", keyword));

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);

        String responseStr = EntityUtils.toString(response.getEntity());

        JSONArray jsonArray = JSONObject.parseObject(responseStr).getJSONObject("hits").getJSONArray("hits");

        List<News> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            JSONObject source = jsonObject.getJSONObject("_source");
            JSONObject highlight = jsonObject.getJSONObject("highlight");

            String title = "";
            String content = "";

            // 从高亮部分获取title,如果没有就从source中获取
            JSONArray titleList = highlight.getJSONArray("title");
            if (titleList == null) {
                title = source.getString("title");
            } else {
                StringBuffer buffer = new StringBuffer();
                titleList.forEach(l -> {
                    buffer.append(l);
                });

                title = buffer.toString();
            }

            // 从高亮部分获取内容, 如果没有就从source中获取
            JSONArray contentList = highlight.getJSONArray("content");
            if (contentList == null) {
                content = source.getString("content");
            } else {
                StringBuffer buffer = new StringBuffer();
                contentList.forEach(l-> {
                    buffer.append(l);
                });
                content = buffer.toString();
            }

            News news = News.builder()
                    .id(source.getInteger("id"))
                    .title(title)
                    .content(content)
                    .url(source.getString("url")).build();

            list.add(news);
        }

        return list;
    }
}
