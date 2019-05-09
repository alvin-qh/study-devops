#!/usr/bin/env bash

curl -X POST http://localhost:9200/study/books/think-in-java?pretty	\
	 -H 'Cache-Control: no-cache' 								\
     -H 'Content-Type: application/json'						\
	 -d '{
			"name": {
				"en": "Think In Java",
				"zh": "Java编程思想"
			}, 
			"intro": "本书赢得了全球程序员的广泛赞誉，即使是最晦涩的概念，在Bruce Eckel的文字亲和力和小而直接的编程示例面前也会化解于无形。从Java的基础语法到最高级特性（深入的面向对象概念、多线程、自动项目构建、单元测试和调试等），本书都能逐步指导你轻松掌握。 从本书获得的各项大奖以及来自世界各地的读者评论中，不难看出这是一本经典之作。本书共22章，包括操作符、控制执行流程、访问权限控制、复用类、多态、接口、通过异常处理错误、字符串、泛型、数组、容器深入研究、JavaI/O系统、枚举类型、并发以及图形化用户界面等内容。这些丰富的内容，包含了Java语言基础语法以及高级特性，适合各个层次的Java程序员阅读，同时也是高等院校讲授面向对象程序设计语言以及Java语言的绝佳教材和参考书。",
			"author": "Bruce Eckel",
			"press": {
				"zh": "机械工业出版社"
			},
			"category": [
				"计算机",
				"软件开发",
				"编程语言",
				"Java语言"
			],
			"publication_date": "2007-06-01"
		}'
