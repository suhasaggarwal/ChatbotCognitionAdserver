# -*- coding: utf-8 -*-
import scrapy
from scrapy.linkextractors import LinkExtractor
from scrapy.spiders import CrawlSpider, Rule
from digitv2.items import Digitv2Item

class Digitv3Spider(CrawlSpider):
    name = 'digitv3'
    allowed_domains = ['www.digit.in']
    
    start_urls = ['https://www.digit.in/top-products/']

    rules = (
        Rule(LinkExtractor(), callback='parse_item', follow=True),
        Rule(LinkExtractor(deny_domains=["geek.digit.in","skoar.digit.in","amazon","flipkart"])),
)

    def parse_item(self, response):
        href = Digitv2Item()
        href['url'] = response.url
        return href
