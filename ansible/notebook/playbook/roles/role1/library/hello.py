import os
from ansible.module_utils.basic import (AnsibleModule)

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

module = AnsibleModule(argument_spec=arg_template)
arg_name = module.params['name']
arg_gender = module.params['gender']

prefix = 'Mr.'
if (arg_gender == 'F'):
    prefix = 'Ms.'

msg = 'Hello {} {}'.format(prefix, arg_name)

os.system('echo {}'.format(msg))

result = {
    'changed': True,
    'msg': msg
}

module.exit_json(**result)
