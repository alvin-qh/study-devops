# 自定义模块

import os
from ansible.module_utils.basic import AnsibleModule

# 模块参数定义模板
# - name: "..."
#   hello:
#     name: Alvin # argument 'name'
#     gender: 'M' # argument 'gender'
arg_template = {
    'name': {
        'type': 'str',
        'required': True
    },
    'gender': {
        'type': 'str',
        'default': 'M'
    }
}

# 创建模块实例，设置参数定义模板
module = AnsibleModule(argument_spec=arg_template)

# 获取传入模块的参数
arg_name = module.params['name']
arg_gender = module.params['gender']

prefix = 'Mr.'
if (arg_gender == 'F'):
    prefix = 'Ms.'

# 设置输出信息
msg = 'Hello {} {}'.format(prefix, arg_name)

# 执行模块命令
os.system('echo {}'.format(msg))

# 设置输出结果
module.exit_json(changed=True, msg=msg)
