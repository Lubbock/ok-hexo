#!/usr/bin/env python
# -*- coding : utf-8-*-
# coding:unicode_escape


import math
# '''文章关键字抽取'''
import os
from collections import Counter
from time import time
import sys
import jieba
import jieba.posseg as pseg
import pandas as pd
from pyecharts.charts import WordCloud

rmcx = ['v', 'vd', 'vn', 'vshi', 'vyou', 'vf', 'vx', 'vi', 'vg',
        'vl', 'a', 'ad', 'an', 'ag', 'al', 'z', 'r', 'rr', 'rz', 'rzt',
        'rzs', 'rzv', 'ry', 'ryt', 'rys', 'ryv', 'rg', 'm', 'mq',
        'q', 'qv', 'qt', 'd', 'u', 'uzhe', 'ule', 'uguo', 'ude1', 'ude2', 'ude3',
        'usuo', 'udeng', 'uyy', 'udh', 'uls', 'uzhi', 'ulian', 'e', 'y', 'o', 'wm',
        ]
jieba.load_userdict("/media/lame/0DD80F300DD80F30/code/ok-hexo/scripts/py-text/resource/jieba.txt")


def transformLine(line):
    words = pseg.lcut(line)
    return [word for word, flag in words if len(word) > 1 and flag not in rmcx]


class OkDoc(object):
    def __init__(self, dirname):
        self.dirname = dirname

    def __iter__(self):
        for root, dirs, files in os.walk(self.dirname):
            for filename in files:
                file_path = os.path.join(root, filename)
                if not file_path.endswith("md"):
                    continue
                doc = []
                for line in open(file_path):
                    try:
                        sline = line.strip()
                        if sline == "":
                            continue
                        tokenized_line = ''.join(sline)
                        doc.append(transformLine(tokenized_line))
                    except Exception:
                        print("catch exception")
                        yield ""
                yield {
                    'name': filename,
                    'doc': doc
                }


class PkDoc(object):

    def __init__(self, dirname):
        self.dirname = dirname

    def __iter__(self):
        for root, dirs, files in os.walk(self.dirname):
            for filename in files:
                file_path = os.path.join(root, filename)
                if not file_path.endswith("md"):
                    continue
                for line in open(file_path):
                    try:
                        sline = line.strip()
                        if sline == "":
                            continue
                        tokenized_line = ''.join(sline)
                        word_line = transformLine(tokenized_line)
                        yield word_line
                    except Exception:
                        print("catch exception")
                        yield ""


class PkSeg(object):

    def __init__(self, content):
        # //段落内容
        self.content = content
        # 段落关键字
        self.keyword = []


def stopwordslist():
    stopwords = [line.strip() for line in
                 open(
                     '/media/lame/0DD80F300DD80F30/code/ok-hexo/scripts/py-text/resource/stopwords/baidu_stopwords.txt',
                     encoding='UTF-8').readlines()]
    return stopwords


# word可以通过count得到，count可以通过countlist得到
# count[word]可以得到每个单词的词频， sum(count.values())得到整个句子的单词总数
def gtf(word, count):
    return count[word] / sum(count.values())


# 统计的是含有该单词的句子数
def gn_containing(word, article):
    return sum(1 for sentence in article if word in sentence)


# len(count_list)是指句子的总数，n_containing(word, count_list)是指含有该单词的句子的总数，加1是为了防止分母为0
def gidf(word, count_list):
    return math.log(len(count_list) / (1 + gn_containing(word, count_list)))


# 将tf和idf相乘
def gtfidf(word, count, count_list):
    f = gidf(word, count_list)
    return gtf(word, count) * f


stopwords = stopwordslist()


def removeStopWord(w):
    if w in stopwords:
        return False
    else:
        return True


def ct(scandir):
    begin = time()
    articles = OkDoc(scandir)
    for article in articles:
        wordsCounter = Counter()
        for s in article['doc']:
            wp = [word.lower() for word in s if
                  all(y not in stopwords for y in s)]
            wordsCounter.update(wp)
        wd = dict(wordsCounter)
        t1 = pd.DataFrame(columns=["kw", "count", "tfidf"])
        t1['kw'] = list(wd.keys())
        t1['count'] = list(wd.values())
        g = [gtfidf(word, wordsCounter, article) for word in wd]
        t1['tfidf'] = g
        t1 = t1.sort_values(by=['tfidf'], ascending=[False])
        t1 = t1.head(20)
        subjects = {}
        for index, row in t1.iterrows():
            subjects[row['kw']] = row['count']
        wordsCounter = Counter(subjects)
        wordslist = wordsCounter.most_common(3)
        print("*********" + article['name'])
        print(",".join(list(zip(*wordslist))[0]))


if __name__ == '__main__':
    args = sys.argv
    ct(args[1])
