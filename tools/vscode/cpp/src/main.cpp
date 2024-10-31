#include <iostream>
#include "text.h"

int main(int argc, char const *argv[])
{
    char buf[128] = "";
    get_string(buf, 128);

    std::cout<<buf<<std::endl;
    return 0;
}
