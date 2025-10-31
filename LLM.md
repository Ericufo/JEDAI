curl --request POST \
  --url https://cloud.infini-ai.com/maas/deepseek-v3.2-exp/nvidia/chat/completions \
  --header "Authorization: Bearer $API_KEY" \
  --header "Content-Type: application/json" \
  --data '{
      "model": "deepseek-v3.2-exp",
      "messages": [
        {"role": "user", "content":"你是谁"}
      ]
    }'



    ⼤模型服务平台（GenStudio）API
Endpoints
Schemas
powered by Stoplight
M×N 多模型芯片 Chat completions
post
https://cloud.infini-ai.com/maas/{model}/{chiptype}/chat/completions
大模型服务平台（GenStudio）提供了专用的多模型芯片（M×N）推理 API，已适配多种国产 AI 芯片。

M×N 多模型芯片 API 服务与 OpenAI Chat Completions 接口兼容，但路径不同于 OpenAI /v1/chat/completions，实际路径以上方所示为准。

API 端点路径中包含两个变量。在构建 API 请求时，您需要根据当前使用的模型和芯片，将变量替换为真实值。

{model} ： 指定使用的模型
{chiptype}： 指定使用的芯片
例如，指定使用 nvidia 芯片，得到的 Server 地址如下：

/maas/megrez-3b-instruct/nvidia/chat/completions
模型字段说明：在此接口中，model 字段为必填字段，支持 string 或 null 类型：

当传入 string 类型时：覆盖路径参数中的模型，使用此处指定的模型 ID
当传入 null 时：使用路径参数中指定的模型
M×N API 服务仅支持部分预置模型。如需查询具体可用组合，请前往 GenStudio 模型广场，按照「支持芯片」进行筛选。
M×N API 服务当前 chip3、chip4、chip5 与 OpenAI 的接口行为存在以下差异：
流式响应模式下，仅在最后一条返回数据中携带携带 object 和 created 字段。
Request
Path Parameters
chiptype
string
required
需要使用的模型，请输入模型 ID (全小写形式)

Allowed values:
nvidia
amd
chip1
chip2
chip3
chip4
chip5
Default:
nvidia
model
string
required
需要使用的模型，请输入模型 ID (全小写形式)

Allowed values:
deepseek-r1-distill-qwen-32b
deepseek-r1
deepseek-v3
deepseek-v3.1
deepseek-v3.1-terminus
deepseek-v3.2-exp
glm-4.5
glm-4.6
glm-4.5-air
glm-4.5v
megrez-3b-instruct
kimi-k2-instruct
qwen3-8b
qwen3-14b
qwen3-32b
qwen3-30b-a3b
qwen3-235b-a22b
qwen3-235b-a22b-instruct-2507
qwen3-vl-235b-a22b-instruct
qwen3-vl-235b-a22b-thinking
qwen3-coder-480b-a35b-instruct
qwen3-next-80b-a3b-instruct
qwen3-next-80b-a3b-thinking
ernie-4.5-21b-a3b
ernie-4.5-300b-a47b
qwen2.5-7b-instruct
qwen2.5-14b-instruct
qwen2.5-32b-instruct
qwen2.5-72b-instruct
qwen2.5-vl-7b-instruct
qwen2.5-vl-72b-instruct
qwq-32b
step3
Body

application/json

application/json
model
string
required
以路径参数中指定的模型为准，此处可传 null 或与路径中相同的模型 ID。此处模型列表可能不完全准确，请以 GenStudio 模型广场实际展示的模型为准。

Allowed values:
deepseek-r1-distill-qwen-32b
deepseek-r1
deepseek-v3
deepseek-v3.1
deepseek-v3.1-terminus
deepseek-v3.2-exp
glm-4.5
glm-4.6
glm-4.5-air
glm-4.5v
megrez-3b-instruct
kimi-k2-instruct
qwen3-8b
qwen3-14b
qwen3-32b
qwen3-30b-a3b
qwen3-235b-a22b
qwen3-235b-a22b-instruct-2507
qwen3-vl-235b-a22b-instruct
qwen3-vl-235b-a22b-thinking
qwen3-coder-480b-a35b-instruct
qwen3-next-80b-a3b-instruct
qwen3-next-80b-a3b-thinking
ernie-4.5-21b-a3b
ernie-4.5-300b-a47b
qwen2.5-7b-instruct
qwen2.5-14b-instruct
qwen2.5-32b-instruct
qwen2.5-72b-instruct
qwen2.5-vl-7b-instruct
qwen2.5-vl-72b-instruct
qwq-32b
step3
Example:
kimi-k2-instruct
messages
array[object]
required
消息列表，遵循 gpt-3.5-turbo 消息规范，同时包含对话上下文的消息内容。

role
string
required
支持 system（系统指令）、user（用户提问）和 assistant（模型回答）。

Allowed values:
system
user
assistant
Default:
user
content
stringarray[object]

one of: string
required
纯文本内容。

tools
array[object]
工具列表。仅部分模型支持 Functional calling，请以模型广场「工具调用」筛选结果为准。

type
string
required
工具类型，目前支持 function

Allowed value:
function
function
object
函数定义

stream
boolean
是否开启流式响应。默认关闭，将一次性返回此次生成的所有内容。

Default:
false
enable_thinking
boolean
是否开启推理能力。默认值与具体推理模型有关。请以具体模型行为为准。

temperature
number<float>
采样控制参数，介于 0 和 2 之间。较高的值将使输出更加随机。0 适用于返回有明确答案的问题，例如 "5 乘以 7 等于多少？"。推荐您根据应用场景调整 top_p 或 temperature 参数，但不建议同时调整两个参数。

>= 0
<= 2
Default:
0.7
top_p
number<float>
采样控制参数，介于 0 和 1 之间。在这种方法中，模型只考虑概率质量最高的前 top_p 的结果。所以 0.1 意味着只有概率质量为前 10% 的 token 被考虑。0 适用于返回有明确答案的问题，例如 "5 乘以 7 等于多少？"。推荐您根据应用场景调整 top_p 或 temperature 参数，但不建议同时调整两个参数。

>= 0
<= 1
Default:
1
top_k
integer
采样控制参数，控制语言模型在生成文本时，从前 k 个 tokens 随机选择。1 适用于返回有明确答案的问题，例如 "5 乘以 7 等于多少？"

llama-3-70b-instruct 暂不支持 top_k 参数。

Default:
-1
n
integer
返回响应的数量，目前固定值为 1，不支持修改。

>= 1
<= 1
Default:
1
max_tokens
integer or null
允许⽣成的 Token 数量上限。

>= 1
Default:
null
stop
array[string] or null
停止序列用于告诉模型何时停止生成输出。 它们允许您隐式控制正在生成的内容的长度。例如，如果您只想用一句话回答问题，则可以使用 . 作为停止序列。 或者，对于单段答案，您可以使用换行作为停止序列。

非兼容性提示：stop 参数仅支持 Array of String 类型。

Default:
null
presence_penalty
number<float>
多样性控制参数，范围 [-2.0,2.0]。根据 Token 是否已在⽣成⽂本出现过进⾏惩罚。若值⼤于0，表示通过惩罚已经在生成文本中出现的词来降低模型中重复用词的可能性。若值⼩于 0，则鼓励重复。

>= -2
<= 2
Example:
0
frequency_penalty
number<float>
多样性控制参数，范围 [-2.0,2.0]。根据⽣成⽂本出现 token 的频率进⾏惩罚。若值⼤于0，表示通过惩罚已经频繁使用的词来降低模型中重复用词的可能性。若值⼩于 0，则鼓励重复。

>= -2
<= 2
Example:
0
Responses
200
400
401
402
404
413
429
503
成功响应。

当 stream 参数设置为 false 时（非流式模式），此端点返回 application/json 类型的响应体。
当 stream 参数设置为 true 时（流式模式），返回 text/event-stream 类型的响应体。流中的每个事件代表完成内容的一个片段。事件格式如下：data: {JSON 对象}。每个事件中的 JSON 对象包含在模式中描述的属性。流以包含 [DONE] 数据的最终片段结束。客户端应监听这些事件并处理它们以构建完整的响应。关于 text/stream 格式，请参考 MDN 文档：https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Event_stream_format
Body
application/jsontext/event-stream

application/json
responses
/
200
非流式响应返回体。

id
string
请求 ID。字符串，服务端生成的唯一标识符。例如：5fe13c4a4ffc4fb884d571195fceab0b。

Example:
5fe13c4a4ffc4fb884d571195fceab0b
object
string
非流式响应（非 stream 模式）时总是返回 chat.completion。

Allowed value:
chat.completion
created
integer
响应 ⽣成的 Unix 时间戳（单位：秒，UTC）。为整型，不包含毫秒，由服务端 ⽣成。建议在同一请求中保持与流式事件一致的时间戳策略（固定或单调递增），请以实际实现为准。

Example:
1757680008
model
string
当前请求中使⽤的模型 ID。

choices
array[object]
响应内容。

index
integer
数组索引值，从 0 开始。

message
object
消息内容。

finish_reason
string
模型停止生成 Token 的原因。

length - 达到最⼤⻓度
stop - 遇到停止序列
Allowed values:
length
stop
usage
object
Token 统计数据。

prompt_tokens
integer
输入的 Token 总数。

completion_tokens
integer
输出的 Token 总数。

total_tokens
integer
本次请求的 Token 总数（含请求和响应）。

prompt_tokens_details
object or null
输入 Token 的详细统计信息。可能为 null。

blocked
boolean
当触发审核违规时，为 true。若未触发审核违规时，不显⽰该字段。

Token
:
123
chiptype*
:
nvidiaamdchip1chip2chip3chip4chip5

nvidia
model*
:
deepseek-r1-distill-qwen-32bdeepseek-r1deepseek-v3deepseek-v3.1deepseek-v3.1-terminusdeepseek-v3.2-expglm-4.5glm-4.6glm-4.5-airglm-4.5vmegrez-3b-instructkimi-k2-instructqwen3-8bqwen3-14bqwen3-32bqwen3-30b-a3bqwen3-235b-a22bqwen3-235b-a22b-instruct-2507qwen3-vl-235b-a22b-instructqwen3-vl-235b-a22b-thinkingqwen3-coder-480b-a35b-instructqwen3-next-80b-a3b-instructqwen3-next-80b-a3b-thinkingernie-4.5-21b-a3bernie-4.5-300b-a47bqwen2.5-7b-instructqwen2.5-14b-instructqwen2.5-32b-instructqwen2.5-72b-instructqwen2.5-vl-7b-instructqwen2.5-vl-72b-instructqwq-32bstep3

deepseek-r1-distill-qwen-32b
{
  "model": "kimi-k2-instruct",
  "messages": [
    {
      "role": "user",
      "content": "9.11 和 9.8 谁大？"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "string",
        "description": "string",
        "parameters": {
          "type": "string",
          "properties": {},
          "required": [
            "string"
          ],
          "additionalProperties": true
        },
        "strict": true
      }
    }
  ],
  "stream": false,
  "enable_thinking": true,
  "temperature": 0.7,
  "top_p": 1,
  "top_k": -1,
  "n": 1,
  "max_tokens": null,
  "stop": null,
  "presence_penalty": 0,
  "frequency_penalty": 0
}
{
  "model": "kimi-k2-instruct",
  "messages": [
    {
      "role": "user",
      "content": "9.11 和 9.8 谁大？"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "string",
        "description": "string",
        "parameters": {
          "type": "string",
          "properties": {},
          "required": [
            "string"
          ],
          "additionalProperties": true
        },
        "strict": true
      }
    }
  ],
  "stream": false,
  "enable_thinking": true,
  "temperature": 0.7,
  "top_p": 1,
  "top_k": -1,
  "n": 1,
  "max_tokens": null,
  "stop": null,
  "presence_penalty": 0,
  "frequency_penalty": 0
}
Send API Request
curl --request POST \
  --url https://cloud.infini-ai.com/maas/deepseek-r1-distill-qwen-32b/nvidia/chat/completions \
  --header 'Accept: application/json, text/event-stream' \
  --header 'Authorization: Bearer 123' \
  --header 'Content-Type: application/json' \
  --data '{
  "model": "kimi-k2-instruct",
  "messages": [
    {
      "role": "user",
      "content": "9.11 和 9.8 谁大？"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "string",
        "description": "string",
        "parameters": {
          "type": "string",
          "properties": {},
          "required": [
            "string"
          ],
          "additionalProperties": true
        },
        "strict": true
      }
    }
  ],
  "stream": false,
  "enable_thinking": true,
  "temperature": 0.7,
  "top_p": 1,
  "top_k": -1,
  "n": 1,
  "max_tokens": null,
  "stop": null,
  "presence_penalty": 0,
  "frequency_penalty": 0
}'
{
  "id": "5fe13c4a4ffc4fb884d571195fceab0b",
  "object": "chat.completion",
  "created": 1757680008,
  "model": "string",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "user",
        "content": "9.11 和 9.8 谁大？"
      },
      "finish_reason": "length"
    }
  ],
  "usage": {
    "prompt_tokens": 0,
    "completion_tokens": 0,
    "total_tokens": 0,
    "prompt_tokens_details": {}
  },
  "blocked": true
}