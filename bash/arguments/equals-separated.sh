#!/usr/bin/env bash

# 使用"等号"作为参数和参数值之间的分隔符, 例如 -n=Alvin 或 --name=Alvin
# 同时支持短参数 (即 -n) 或长参数 (即 --name)

function help () {
  cat << EOF
Use equals-separated.sh [-n|--name=<NAME>] [-e|--email=<EMAIL>] [-r|--role=<ROLE>] [--default] <PARAMETER>
    NAME: Name of user
    EMAIL: Email of user
    ROLE: Role of user, can be 'USER|ADMIN'
    --default: If use default settings
    PARAMETER: Paramter of command

Example:
    equals-separated.sh --name="Alvin" --email="Email" --default "Hello Bash"
EOF

  exit 1
}

#设置具有缺省值的参数变量
_ROLE="USER"

# 保存非 - 和 -- 开头的参数值
_POSITIONAL_ARGS=()

# $@ 表示参数数组, 即遍历参数数组
for i in "$@"; do
  case $1 in
    -n=*|--name=*)
      _NAME="${i#*=}"
      shift # 跳过参数值
      ;;
    -e=*|--email=*)
      _EMAIL="${i#*=}"
      shift # 跳过参数值
      ;;
    -r=*|--role=*)
      _ROLE="${i#*=}"
      shift # 跳过参数值
      ;;
    --default)
      _DEFAULT=YES
      shift # 跳过参数
      ;;
    -*|--*) # 其它以 - 或 -- 开头的参数
      echo "Unknown option '$1'"
      help
      ;;
    *)
      # 当参数不为上述值时, 将当前参数 ($1) 保存到 _POSITIONAL_ARGS 数组中
      _POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

# 仅将 _POSITIONAL_ARGS 数组的内容重新设置为命令参数, 即 $1, $2, ...
set -- "${_POSITIONAL_ARGS[@]}"

cat << EOF
NAME      = $_NAME
EMAIL     = $_EMAIL
ROLE      = $_ROLE
DEFAULT   = $_DEFAULT
PARAMETER = $1
EOF
