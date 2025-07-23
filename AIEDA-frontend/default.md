# AI EDA API


**简介**:AI EDA API


**HOST**:http://localhost:8080


**联系人**:SparkNJU


**Version**:v1


**接口路径**:/v3/api-docs/default


[TOC]






# 聊天管理


## 发送消息并获取AI回复


**接口地址**:`/api/chats/messages`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>向指定会话发送用户消息，并获取AI回复</p>



**请求示例**:


```javascript
{
  "uid": 0,
  "content": "",
  "sid": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|chatRequestVO|聊天消息请求对象|body|true|ChatRequestVO|ChatRequestVO|
|&emsp;&emsp;uid|||false|integer(int32)||
|&emsp;&emsp;content|||false|string||
|&emsp;&emsp;sid|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseRecordVO|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||RecordVO|RecordVO|
|&emsp;&emsp;rid||integer(int32)||
|&emsp;&emsp;sid||integer(int32)||
|&emsp;&emsp;direction||boolean||
|&emsp;&emsp;content||string||
|&emsp;&emsp;sequence||integer(int32)||
|&emsp;&emsp;type||integer(int32)||
|&emsp;&emsp;createTime||string(date-time)||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": {
		"rid": 0,
		"sid": 0,
		"direction": true,
		"content": "",
		"sequence": 0,
		"type": 0,
		"createTime": ""
	}
}
```


## 创建新会话


**接口地址**:`/api/chats/sessions`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建一个新的聊天会话</p>



**请求示例**:


```javascript
{
  "uid": 0,
  "title": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createSessionRequestVO|创建会话请求对象|body|true|CreateSessionRequestVO|CreateSessionRequestVO|
|&emsp;&emsp;uid|||false|integer(int32)||
|&emsp;&emsp;title|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseSessionVO|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||SessionVO|SessionVO|
|&emsp;&emsp;sid||integer(int32)||
|&emsp;&emsp;title||string||
|&emsp;&emsp;createTime||string(date-time)||
|&emsp;&emsp;updateTime||string(date-time)||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": {
		"sid": 0,
		"title": "",
		"createTime": "",
		"updateTime": ""
	}
}
```


## 获取会话记录


**接口地址**:`/api/chats/sessions/{sid}/records`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据会话ID获取所有聊天记录</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|sid|会话ID|path|true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseListRecordVO|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||array|RecordVO|
|&emsp;&emsp;rid||integer(int32)||
|&emsp;&emsp;sid||integer(int32)||
|&emsp;&emsp;direction||boolean||
|&emsp;&emsp;content||string||
|&emsp;&emsp;sequence||integer(int32)||
|&emsp;&emsp;type||integer(int32)||
|&emsp;&emsp;createTime||string(date-time)||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": [
		{
			"rid": 0,
			"sid": 0,
			"direction": true,
			"content": "",
			"sequence": 0,
			"type": 0,
			"createTime": ""
		}
	]
}
```


## 获取用户会话列表


**接口地址**:`/api/chats/sessions/{uid}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据用户ID获取所有聊天会话</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|uid|用户ID|path|true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ResponseListSessionVO|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|message||string||
|data||array|SessionVO|
|&emsp;&emsp;sid||integer(int32)||
|&emsp;&emsp;title||string||
|&emsp;&emsp;createTime||string(date-time)||
|&emsp;&emsp;updateTime||string(date-time)||


**响应示例**:
```javascript
{
	"code": "",
	"message": "",
	"data": [
		{
			"sid": 0,
			"title": "",
			"createTime": "",
			"updateTime": ""
		}
	]
}
```


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