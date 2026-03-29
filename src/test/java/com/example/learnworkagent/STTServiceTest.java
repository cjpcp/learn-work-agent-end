package com.example.learnworkagent;

import com.example.learnworkagent.infrastructure.external.dify.SpeechToTextService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class STTServiceTest {

    @Resource
    private SpeechToTextService speechToTextService;


    @Test
    public void test() {
        String s = speechToTextService.convertVoiceUrlToText("https://learn-work-agent-end.oss-cn-beijing.aliyuncs.com/consultation/voice/36%E4%B8%A8%20Redis%E6%94%AF%E6%92%91%E7%A7%92%E6%9D%80%E5%9C%BA%E6%99%AF%E7%9A%84%E5%85%B3%E9%94%AE%E6%8A%80%E6%9C%AF%E5%92%8C%E5%AE%9E%E8%B7%B5%E9%83%BD%E6%9C%89%E5%93%AA%E4%BA%9B%EF%BC%9F.wav", "test");
        System.out.println(s);
    }


}
