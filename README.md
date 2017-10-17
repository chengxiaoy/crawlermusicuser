# crawlermusicuser

spring boot+mongodb  
爬取网易云音乐的用户信息和歌曲信息  
网易云音乐webapi 的请求参数都使用了aes和ras的加密  
参数通过chrome调试得到


# 九月15号
1. 根据用户id获取用户基本信息
2. 根据用户id获取粉丝和关注人id
3. 获取用户喜欢的歌曲（变化的数据）
4. 对于获取的id数据如何去重？_**Bloom Filter redis**_
5. 遍历用户广度优先？

# 九月30号
1. 爬取了20w用户以及用户播放次数最多的前一百首歌曲的信息
2. 下一步使用python读取mongodb数据
3. 可以做的事情有 一些数据的统计  最受喜欢的歌曲,歌手 最受喜欢的作词家 
4. 朴素贝叶斯猜测歌词属于哪个作词者
5. 为自己推荐一些可能喜欢的歌曲


在爬取的用户中，最受喜欢的十首歌

|歌曲名称 | 演唱者 | 喜欢人数| 
|--------|-------|------|
|童话镇|陈一发儿|7322|
|成都|赵雷|6226|
|追光者|岑宁儿|5589|
|刚好遇见你|李玉刚|5403|
|演员|薛之谦|4628|
|我的一个道姑朋友（Cover Lon）|	以冬|4485|
|理想三旬	|陈鸿宇|	4388|
|岁月神偷	|金玟岐|	4174|
|告白气球	|周杰伦|	4044|
|再也没有	|Ryan.B|3825|

绘制的柱形图
从每个人最喜欢的十首歌来统计：
<img src="https://user-images.githubusercontent.com/19379550/31659223-fca53eb2-b2f8-11e7-867f-4504094c1e31.png" align=center width="800px" height="650px"/>
扩大到每个人最喜欢的一百首歌
<img src="https://user-images.githubusercontent.com/19379550/31659260-2247c554-b2f9-11e7-9ce2-2c9d21120cd8.png" align=center width="800px" height="650px"/>
最受欢迎的作词人
<img src="https://user-images.githubusercontent.com/19379550/31659273-2c2e039e-b2f9-11e7-9d6f-7ef095f1522e.png" align=center width="800px" height="650px"/>
最受欢迎的作曲人
<img src="https://user-images.githubusercontent.com/19379550/31659282-3638f2d6-b2f9-11e7-9432-5f318e7ffbdc.png" align=center width="800px" height="650px"/>



下一步 寻找和自己听歌口味近似的人