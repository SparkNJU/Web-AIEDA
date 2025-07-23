# AI EDA API


**简介**:AI EDA API


**HOST**:http://localhost:8080


**联系人**:SparkNJU


**Version**:v1


**接口路径**:/v3/api-docs/default


[TOC]






# 账户管理


## 用户注册


**接口地址**:`/api/accounts`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>注册一个新用户</p>



**请求示例**:


```javascript
{
  "username": "",
  "phone": "",
  "password": "",
  "description": "",
  "role": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|accountVO|用户登录信息|body|true|AccountVO|AccountVO|
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;role|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||string||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": ""
}
```


## 更新用户信息


**接口地址**:`/api/accounts`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新用户的详细信息</p>



**请求示例**:


```javascript
{
  "uid": 0,
  "username": "",
  "phone": "",
  "password": "",
  "description": "",
  "role": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|account|用户信息对象|body|true|Account|Account|
|&emsp;&emsp;uid|||false|integer(int32)||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;role|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||string||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": ""
}
```


## 删除用户


**接口地址**:`/api/accounts`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据手机号删除用户</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|phone|用户手机号|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||string||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": ""
}
```


## 获取用户详情


**接口地址**:`/api/accounts/{phone}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据手机号获取用户详细信息</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|phone|用户手机号|path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseAccountVO|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||AccountVO|AccountVO|
|&emsp;&emsp;username||string||
|&emsp;&emsp;phone||string||
|&emsp;&emsp;password||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;role||integer(int32)||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": {
		"username": "",
		"phone": "",
		"password": "",
		"description": "",
		"role": 0
	}
}
```


## 用户登录


**接口地址**:`/api/accounts/login`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>用户登录接口</p>



**请求示例**:


```javascript
{
  "username": "",
  "phone": "",
  "password": "",
  "description": "",
  "role": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|accountVO|用户登录信息|body|true|AccountVO|AccountVO|
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;phone|||false|string||
|&emsp;&emsp;password|||false|string||
|&emsp;&emsp;description|||false|string||
|&emsp;&emsp;role|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||string||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": ""
}
```