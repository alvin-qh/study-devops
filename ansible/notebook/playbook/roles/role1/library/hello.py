import os
from ansible.module_utils.basic import (AnsibleModule)

# Arguments template of module, use:
# - name: "..."
#   hello:
#     name: Alvin   # argument 'name'
#     gender: 'M'   # argument 'gender'
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

# create ansible module, setup module argument
module = AnsibleModule(argument_spec=arg_template)

# get argument values
arg_name = module.params['name']
arg_gender = module.params['gender']

prefix = 'Mr.'
if (arg_gender == 'F'):
    prefix = 'Ms.'

# make output string
msg = 'Hello {} {}'.format(prefix, arg_name)

# run command on remote server
os.system('echo {}'.format(msg))

# exit module and report result
module.exit_json(changed=True, msg=msg)
