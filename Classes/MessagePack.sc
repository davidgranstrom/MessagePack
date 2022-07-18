// https://github.com/msgpack/msgpack/blob/master/spec.md

MessagePack {
    classvar <>defaultInitialSize = 2048;
    classvar <>defaultMaxDepth = 256;
    classvar <maxSize = 4294967296.0; // 2^32

    *encode {arg object, options;
        ^MessagePackEncoder(options).encode(object);
    }

    *decode {arg data, options;
        ^MessagePackDecoder(options).decode(data);
    }

    *print {arg data;
        ^data.collect {arg byte;
            byte.asHexString(2).toLower;
        };
    }
}
