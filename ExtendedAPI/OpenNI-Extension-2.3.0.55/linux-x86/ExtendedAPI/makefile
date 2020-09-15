CC = g++

INCPATH = -I./3rdparty/OpenNI2/Include

LIBPATH = -L./3rdparty/OpenNI2/Lib \
				-L./3rdparty/OpenNI2/Redist \
	  	  
LDLIBS += -lOpenNI2 -lusb-1.0 -lGL -lGLU -lglut

CFLAGS=-O0 -DLINUX -g -Wall -std=c++11 -fpermissive
LDFLAGS += -Wl,-rpath=.

SOURCE = ./*.cpp

TARGET = ExtendedAPI

all:
	$(CC) $(CFLAGS)  $(INCPATH) $(LIBPATH)  $(SOURCE) $(LDFLAGS) $(LDLIBS) -lpthread  -o $(TARGET)
	mkdir bin
	cp -r ./3rdparty/OpenNI2/Redist/*  ./bin
	mv $(TARGET) ./bin
clean:
	rm -f *.o $(TARGET) *~

