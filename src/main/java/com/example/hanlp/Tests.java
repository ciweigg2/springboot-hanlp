package com.example.hanlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.suggest.Suggester;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.SpeedTokenizer;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author 马秀成
 * @date 2018/11/1
 * @jdk.version 1.8
 * @desc
 */
public class Tests {

    /**
     * 分词功能
     */
    @Test
    public void testSegment() {
        System.out.println(HanLP.segment("www.codesheep.cn是一个技术博客！"));
    }

    /**
     * 文本推荐
     * 三个关键字的语句推荐结果为：
     * 机器学习  →  [人工智能如今是非常火热的一门技术”]
     * 危机公共  →  [威廉王子发表演说 呼吁保护野生动物]
     * mayun     →  [《时代》年度人物最终入围名单出炉 普京马云入选]
     */
    @Test
    public void testSuggest() {
        Suggester suggester = new Suggester();
        String[] titleArray =
                (
                        "威廉王子发表演说 呼吁保护野生动物\n" +
                                "《时代》年度人物最终入围名单出炉 普京马云入选\n" +
                                "“黑格比”横扫菲：菲吸取“海燕”经验及早疏散\n" +
                                "日本保密法将正式生效 日媒指其损害国民知情权\n" +
                                "人工智能如今是非常火热的一门技术”"
                ).split("\\n");
        for (String title : titleArray) {
            suggester.addSentence(title);
        }

        System.out.println(suggester.suggest("机器学习", 1));   // 语义
        System.out.println(suggester.suggest("危机公共", 1));   // 字符
        System.out.println(suggester.suggest("mayun", 1));     // 拼音
    }

    /**
     * 关键字提取
     */
    @Test
    public void testKeyExtract() {
        String content = "苹果公司（Apple Inc. ）是美国一家高科技公司。由史蒂夫·乔布斯、斯蒂夫·沃兹尼亚克和罗·韦恩(Ron Wayne)等人于1976年4月1日创立，" +
                "并命名为美国苹果电脑公司（Apple Computer Inc. ），2007年1月9日更名为苹果公司，总部位于加利福尼亚州的库比蒂诺。";
        List<String> keywordList = HanLP.extractKeyword(content, 5);
        System.out.println(keywordList);
    }

    /**
     * 标准分词
     */
    @Test
    public void segment() {
        List<Term> termList = StandardTokenizer.segment("商品和服务");
        System.out.println(termList);
    }

    /**
     * NLP分词
     * NLP分词NLPTokenizer会执行词性标注和命名实体识别
     */
    @Test
    public void segmentNLP() {
        System.out.println(NLPTokenizer.segment("我新造一个词叫幻想乡你能识别并标注正确词性吗？"));
        // 注意观察下面两个“希望”的词性、两个“晚霞”的词性
        System.out.println(NLPTokenizer.analyze("我的希望是希望张晚霞的背影被晚霞映红").translateLabels());
        System.out.println(NLPTokenizer.analyze("支援臺灣正體香港繁體：微软公司於1975年由比爾·蓋茲和保羅·艾倫創立。"));
    }

    /**
     * 索引分词
     * 索引分词IndexTokenizer是面向搜索引擎的分词器，能够对长词全切分，另外通过term.offset可以获取单词在文本中的偏移量。
     * 任何分词器都可以通过基类Segment的enableIndexMode方法激活索引模式。
     */
    @Test
    public void IndexTokenizer() {
        List<Term> termList = IndexTokenizer.segment("主副食品");
        for (Term term : termList) {
            System.out.println(term + " [" + term.offset + ":" + (term.offset + term.word.length()) + "]");
        }
    }

    /**
     * N-最短路径分词
     * N最短路分词器NShortSegment比最短路分词器慢，但是效果稍微好一些，对命名实体识别能力更强。
     * 一般场景下最短路分词的精度已经足够，而且速度比N最短路分词器快几倍，请酌情选择。
     */
    @Test
    public void n() {
        Segment nShortSegment = new NShortSegment().enableCustomDictionary(false).enablePlaceRecognize(true).enableOrganizationRecognize(true);
        Segment shortestSegment = new DijkstraSegment().enableCustomDictionary(false).enablePlaceRecognize(true).enableOrganizationRecognize(true);
        String[] testCase = new String[]{
                "今天，刘志军案的关键人物,山西女商人丁书苗在市二中院出庭受审。",
                "刘喜杰石国祥会见吴亚琴先进事迹报告团成员",
        };
        for (String sentence : testCase) {
            System.out.println("N-最短分词：" + nShortSegment.seg(sentence) + "\n最短路分词：" + shortestSegment.seg(sentence));
        }
    }

    /**
     * CRF分词
     */
    @Test
    public void crf() throws IOException {
        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        String[] tests = new String[]{
                "商品和服务",
                "上海华安工业（集团）公司董事长谭旭光和秘书胡花蕊来到美国纽约现代艺术博物馆参观",
                "微软公司於1975年由比爾·蓋茲和保羅·艾倫創立，18年啟動以智慧雲端、前端為導向的大改組。" // 支持繁体中文
        };
        for (String sentence : tests) {
            System.out.println(analyzer.analyze(sentence));
        }
    }

    /**
     * 极速词典分词
     * 演示极速分词，基于AhoCorasickDoubleArrayTrie实现的词典分词，适用于“高吞吐量”“精度一般”的场合
     * 极速分词是词典最长分词，速度极其快，精度一般。
     * 在i7-6700K上跑出了4500万字每秒的速度。
     */
    @Test
    public void demoHighSpeedSegment() {
        String text = "江西鄱阳湖干枯，中国最大淡水湖变成大草原";
        System.out.println(SpeedTokenizer.segment(text));
        long start = System.currentTimeMillis();
        int pressure = 1000000;
        for (int i = 0; i < pressure; ++i)
        {
            SpeedTokenizer.segment(text);
        }
        double costTime = (System.currentTimeMillis() - start) / (double)1000;
        System.out.printf("分词速度：%.2f字每秒", text.length() * pressure / costTime);
    }

    /**
     * 用户自定义词典
     * 演示用户词典的动态增删
     * 说明
     * CustomDictionary是一份全局的用户自定义词典，可以随时增删，影响全部分词器。另外可以在任何分词器中关闭它。通过代码动态增删不会保存到词典文件。
     * 中文分词≠词典，词典无法解决中文分词，Segment提供高低优先级应对不同场景，请参考FAQ。
     * 追加词典
     * CustomDictionary主词典文本路径是data/dictionary/custom/CustomDictionary.txt，用户可以在此增加自己的词语（不推荐）；也可以单独新建一个文本文件，通过配置文件CustomDictionaryPath=data/dictionary/custom/CustomDictionary.txt; 我的词典.txt;来追加词典（推荐）。
     * 始终建议将相同词性的词语放到同一个词典文件里，便于维护和分享。
     * 词典格式
     * 每一行代表一个单词，格式遵从[单词] [词性A] [A的频次] [词性B] [B的频次] ... 如果不填词性则表示采用词典的默认词性。
     * 词典的默认词性默认是名词n，可以通过配置文件修改：全国地名大全.txt ns;如果词典路径后面空格紧接着词性，则该词典默认是该词性。
     * 在统计分词中，并不保证自定义词典中的词一定被切分出来。用户可在理解后果的情况下通过Segment#enableCustomDictionaryForcing强制生效。
     * 关于用户词典的更多信息请参考词典说明一章。
     * @throws IOException
     */
    @Test
    public void demoCustomDictionary() {
        // 动态增加
        CustomDictionary.add("攻城狮");
        // 强行插入
        CustomDictionary.insert("白富美", "nz 1024");
        // 删除词语（注释掉试试）
//        CustomDictionary.remove("攻城狮");
        System.out.println(CustomDictionary.add("单身狗", "nz 1024 n 1"));
        System.out.println(CustomDictionary.get("单身狗"));

        String text = "攻城狮逆袭单身狗，迎娶白富美，走上人生巅峰";  // 怎么可能噗哈哈！

        // AhoCorasickDoubleArrayTrie自动机扫描文本中出现的自定义词语
        final char[] charArray = text.toCharArray();
        CustomDictionary.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute>()
        {
            @Override
            public void hit(int begin, int end, CoreDictionary.Attribute value)
            {
                System.out.printf("[%d:%d]=%s %s\n", begin, end, new String(charArray, begin, end - begin), value);
            }
        });

        // 自定义词典在所有分词器中都有效
        System.out.println(HanLP.segment(text));
    }

    /**
     * 中国人名识别
     */
    @Test
    public void hmmViterbi() {
        String[] testCase = new String[]{
                "签约仪式前，秦光荣、李纪恒、仇和等一同会见了参加签约的企业家。",
                "王国强、高峰、汪洋、张朝阳光着头、韩寒、小四",
                "张浩和胡健康复员回家了",
                "王总和小丽结婚了",
                "编剧邵钧林和稽道青说",
                "这里有关天培的有关事迹",
                "龚学平等领导,邓颖超生前",
        };
        Segment segment = HanLP.newSegment().enableNameRecognize(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }
    }

    /**
     * 音译人名识别
     */
    @Test
    public void hmmViterbi2() {
        String[] testCase = new String[]{
                "一桶冰水当头倒下，微软的比尔盖茨、Facebook的扎克伯格跟桑德博格、亚马逊的贝索斯、苹果的库克全都不惜湿身入镜，这些硅谷的科技人，飞蛾扑火似地牺牲演出，其实全为了慈善。",
                "世界上最长的姓名是简森·乔伊·亚历山大·比基·卡利斯勒·达夫·埃利奥特·福克斯·伊维鲁莫·马尔尼·梅尔斯·帕特森·汤普森·华莱士·普雷斯顿。",
        };
        Segment segment = HanLP.newSegment().enableTranslatedNameRecognize(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }
    }

    /**
     * 日本人名识别
     */
    @Test
    public void hmmViterbi3() {
        String[] testCase = new String[]{
                "北川景子参演了林诣彬导演的《速度与激情3》",
                "林志玲亮相网友:确定不是波多野结衣？",
        };
        Segment segment = HanLP.newSegment().enableJapaneseNameRecognize(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }
    }

    /**
     * 地名识别
     */
    @Test
    public void hmmViterbi4() {
        String[] testCase = new String[]{
                "武胜县新学乡政府大楼门前锣鼓喧天",
                "蓝翔给宁夏固原市彭阳县红河镇黑牛沟村捐赠了挖掘机",
        };
        Segment segment = HanLP.newSegment().enablePlaceRecognize(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }
    }

    /**
     * 机构名识别
     */
    @Test
    public void hmmViterbi5() {
        String[] testCase = new String[]{
                "我在上海林原科技有限公司兼职工作，",
                "我经常在台川喜宴餐厅吃饭，",
                "偶尔去地中海影城看电影。",
        };
        Segment segment = HanLP.newSegment().enableOrganizationRecognize(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
            System.out.println(termList);
        }
    }

    /**
     * 关键词提取
     */
    @Test
    public void hmmViterbi6() {
        String content = "程序员(英文Programmer)是从事程序开发、维护的专业人员。一般将程序员分为程序设计人员和程序编码人员，但两者的界限并不非常清楚，特别是在中国。软件从业人员分为初级程序员、高级程序员、系统分析员和项目经理四大类。";
        List<String> keywordList = HanLP.extractKeyword(content, 5);
        System.out.println(keywordList);
    }

    /**
     * 自动摘要
     */
    @Test
    public void hmmViterbi7() {
        String document = "算法可大致分为基本算法、数据结构的算法、数论算法、计算几何的算法、图的算法、动态规划以及数值分析、加密算法、排序算法、检索算法、随机化算法、并行算法、厄米变形模型、随机森林算法。\n" +
                "算法可以宽泛的分为三类，\n" +
                "一，有限的确定性算法，这类算法在有限的一段时间内终止。他们可能要花很长时间来执行指定的任务，但仍将在一定的时间内终止。这类算法得出的结果常取决于输入值。\n" +
                "二，有限的非确定算法，这类算法在有限的时间内终止。然而，对于一个（或一些）给定的数值，算法的结果并不是唯一的或确定的。\n" +
                "三，无限的算法，是那些由于没有定义终止定义条件，或定义的条件无法由输入的数据满足而不终止运行的算法。通常，无限算法的产生是由于未能确定的定义终止条件。";
        List<String> sentenceList = HanLP.extractSummary(document, 3);
        System.out.println(sentenceList);
    }

    /**
     * 拼音转换
     * HanLP不仅支持基础的汉字转拼音，还支持声母、韵母、音调、音标和输入法首字母首声母功能。
     * HanLP能够识别多音字，也能给繁体中文注拼音。
     * 最重要的是，HanLP采用的模式匹配升级到AhoCorasickDoubleArrayTrie，性能大幅提升，能够提供毫秒级的响应速度！
     */
    @Test
    public void hmmViterbi8() {
        String text = "重载不是重任";
        List<Pinyin> pinyinList = HanLP.convertToPinyinList(text);
        System.out.print("原文,");
        for (char c : text.toCharArray())
        {
            System.out.printf("%c,", c);
        }
        System.out.println();

        System.out.print("拼音（数字音调）,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin);
        }
        System.out.println();

        System.out.print("拼音（符号音调）,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getPinyinWithToneMark());
        }
        System.out.println();

        System.out.print("拼音（无音调）,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getPinyinWithoutTone());
        }
        System.out.println();

        System.out.print("声调,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getTone());
        }
        System.out.println();

        System.out.print("声母,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getShengmu());
        }
        System.out.println();

        System.out.print("韵母,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getYunmu());
        }
        System.out.println();

        System.out.print("输入法头,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getHead());
        }
        System.out.println();
    }

    /**
     * 简繁转换
     */
    @Test
    public void hmmViterbi9() {
        System.out.println(HanLP.convertToTraditionalChinese("用笔记本电脑写程序"));
        System.out.println(HanLP.convertToSimplifiedChinese("「以後等妳當上皇后，就能買士多啤梨慶祝了」"));
    }

    /**
     * 依存句法分析
     */
    @Test
    public void hmmViterbi10() {
        CoNLLSentence sentence = HanLP.parseDependency("徐先生还具体帮助他确定了把画雄鹰、松鼠和麻雀作为主攻目标。");
        System.out.println(sentence);
        // 可以方便地遍历它
        for (CoNLLWord word : sentence)
        {
            System.out.printf("%s --(%s)--> %s\n", word.LEMMA, word.DEPREL, word.HEAD.LEMMA);
        }
        // 也可以直接拿到数组，任意顺序或逆序遍历
        CoNLLWord[] wordArray = sentence.getWordArray();
        for (int i = wordArray.length - 1; i >= 0; i--)
        {
            CoNLLWord word = wordArray[i];
            System.out.printf("%s --(%s)--> %s\n", word.LEMMA, word.DEPREL, word.HEAD.LEMMA);
        }
        // 还可以直接遍历子树，从某棵子树的某个节点一路遍历到虚根
        CoNLLWord head = wordArray[12];
        while ((head = head.HEAD) != null)
        {
            if (head == CoNLLWord.ROOT) {
                System.out.println(head.LEMMA);
            } else{
                System.out.printf("%s --(%s)--> ", head.LEMMA, head.DEPREL);
            };
        }
    }

}
