// https://github.com/msgpack/msgpack/blob/master/spec.md

MessagePack {
    classvar <>defaultInitialSize = 2048;
    classvar <>defaultMaxDepth = 256;
    classvar <maxSize = 4294967296.0; // 2^32
    classvar <extensions;

    *initClass {
        extensions = Array.newClear(128);
    }

    *encode {arg object, options;
        ^MessagePackEncoder(options).encode(object);
    }

    *decode {arg data, options;
        ^MessagePackDecoder(options).decode(data);
    }

    *registerExtension {arg ext;
        extensions = extensions.put(ext.type, ext);
    }

    *print {arg data;
        ^data.collect {arg byte;
            byte.asHexString(2).toLower;
        };
    }
}

MessagePackExt {
    var <type, <name, <>encodeFunc, <>decodeFunc;

    *new {arg type, name = "", encodeFunc, decodeFunc;
        if (type < 0 or:{ type > 127 }) {
            Error("Extension type must be in range 0 - 127").throw;
        };
        ^super.newCopyArgs(type, name, encodeFunc, decodeFunc);
    }

    prEncode {arg object, data;
        if (encodeFunc.isNil) { ^nil };
        ^encodeFunc.(object, data);
    }

    prDecode {arg data;
        if (decodeFunc.isNil) { ^nil };
        ^decodeFunc.(data);
    }
}
