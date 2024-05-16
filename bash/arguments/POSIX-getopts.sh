
#!/usr/bin/env bash

# 使用 getopts 获取参数和参数值
# 注意, 这种方法只能使用短参数 (即 -n)

function help () {
  cat << EOF
Use POSIX-getopts.sh [-n <NAME>] [-e <EMAIL>] [-r <ROLE>] [--d] <PARAMETER>
    NAME: Name of user
    EMAIL: Email of user
    ROLE: Role of user, can be 'USER|ADMIN'
    -d: If use default settings
    PARAMETER: Paramter of command

Example:
    POSIX-getopts.sh -n "Alvin" -e "Email" -d "Hello Bash"
EOF

  exit 1
}

#设置具有缺省值的参数变量
_ROLE="USER"

# 表示获取参数的当前位置, 一开始为 1 表示从第一个参数开始
OPTIND=1

# getopts 表示按指定模式获取参数, 并将参数名赋予 opt 变量
# 本例中包含 h, n, e, r, d 这几个参数, 其中 n:, e:, r: 表示这几个参数需要有参数值
while getopts "hn:e:r:d" opt; do
  case "$opt" in
    h)
      help
      ;;
    n)
      _NAME="$OPTARG"
      ;;
    e)
      _EMAIL="$OPTARG"
      ;;
    r)
      _ROLE="$OPTARG"
      ;;
    d)
      _DEFAULT=YES
      ;;
  esac
done

# 将已经处理的参数从参数列表中移除, 即将当前参数位置之前的参数全部移除
shift $((OPTIND-1))

# 如果当前参数是以 -- 开头的, 也一并移除
[ "${1:-}" = "--" ] && shift

# 完成已处理参数的移除后, 参数列表剩余的即非 - 开头的参数, 通过 $@ 变量获取

cat << EOF
NAME      = $_NAME
EMAIL     = $_EMAIL
ROLE      = $_ROLE
DEFAULT   = $_DEFAULT
PARAMETER = $@
EOF
