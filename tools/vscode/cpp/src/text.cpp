#include <cstddef>
#include <cstring>
#include "text.h"

const char _str[] = "Hello CPP";

int get_string(char* buffer, size_t buf_len)
{
    if (buf_len < sizeof(_str) / sizeof(_str[0])) {
        return -1;
    }
    strcpy(buffer, _str);
    return 0;
}
