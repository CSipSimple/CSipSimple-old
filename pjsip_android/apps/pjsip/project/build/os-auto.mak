# build/os-auto.mak.  Generated from os-auto.mak.in by configure.

export OS_CFLAGS   := $(CC_DEF)PJ_AUTOCONF=1 -O2

export OS_CXXFLAGS := $(CC_DEF)PJ_AUTOCONF=1 -O2 

export OS_LDFLAGS  :=  -lm -luuid -lnsl -lrt -lpthread  -lasound -lcrypto -lssl -lopencore-amrnb

export OS_SOURCES  := 


