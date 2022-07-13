// https://github.com/msgpack/msgpack/blob/master/spec.md

MessagePack {
    classvar <>defaultInitialSize = 2048;
    classvar <>defaultMaxDepth = 100;
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

MessagePackEncoder {
    var <>options;
    var initialSize, maxDepth;
    var integerAsFloat;
    var forceFloat32;
    var buffer, pos;

    *new {arg options;
        ^super.newCopyArgs(options).init;
    }

    init {
        options = options ? ();
        initialSize = options.defaultInitialSize ? MessagePack.defaultInitialSize;
        maxDepth = options.defaultMaxDepth ? MessagePack.defaultMaxDepth;
        integerAsFloat = options.integerAsFloat ? false;
        forceFloat32 = options.forceFloat32 ? false;
    }

    reset {
        buffer = Array.newClear(initialSize);
        pos = 0;
    }

    checkSize {arg size;
        while { pos + size > buffer.size } {
            buffer = buffer.growClear(initialSize);
        }
    }

    writeU8 {arg value;
        this.checkSize(1);
        buffer[pos] = value;
        pos = pos + 1;
    }

    writeU16 {arg value;
        this.checkSize(2);
        value = value.asInteger;
        buffer[pos] = (value >> 8) & 0xff;
        buffer[pos + 1] = value & 0xff;
        pos = pos + 2;
    }

    writeI32 {arg value;
        this.checkSize(4);
        buffer[pos] = floor(value / 0x1000000) % 0x100;
        buffer[pos + 1] = floor(value / 0x10000) % 0x100;
        buffer[pos + 2] = floor(value / 0x100) % 0x100;
        buffer[pos + 3] = value % 0x100;
        pos = pos + 4;
    }

    writeF32 {arg value;
        this.checkSize(4);
        value = value.as32Bits;
        buffer[pos] = (value >> 24) & 0xff;
        buffer[pos + 1] = (value >> 16) & 0xff;
        buffer[pos + 2] = (value >> 8) & 0xff;
        buffer[pos + 3] = value & 0xff;
        pos = pos + 4;
    }

    writeF64 {arg value;
        var hiWord, loWord;
        this.checkSize(8);
        hiWord = value.high32Bits;
        loWord = value.low32Bits;
        buffer[pos + 0] = (hiWord >> 24) & 0xff;
        buffer[pos + 1] = (hiWord >> 16) & 0xff;
        buffer[pos + 2] = (hiWord >> 8)  & 0xff;
        buffer[pos + 3] = hiWord & 0xff;
        buffer[pos + 4] = (loWord >> 24) & 0xff;
        buffer[pos + 5] = (loWord >> 16) & 0xff;
        buffer[pos + 6] = (loWord >> 8)  & 0xff;
        buffer[pos + 7] = loWord & 0xff;
        pos = pos + 8;
    }

    writeString {arg value;
        var bytes;
        bytes = value.ascii % 0x100;
        this.checkSize(bytes.size);
        bytes.do {arg byte;
            buffer[pos] = byte;
            pos = pos + 1;
        };
    }

    encode {arg object;
        this.reset;
        this.prEncode(object, 1);
        buffer = buffer.copyRange(0, pos - 1).asInteger;
        ^buffer;
    }

    prEncode {arg object, depth = 1;
        if (depth > maxDepth) {
            "Too many nested levels".error;
        };
        case
        { object.isKindOf(Nil) } {
            this.encodeNil();
        }
        { object.isKindOf(Boolean) } {
            this.encodeBoolean(object);
        }
        { object.isKindOf(Number) } {
            this.encodeNumber(object);
        }
        { object.isKindOf(String) or:{object.isKindOf(Symbol)}} {
            this.encodeString(object.asString);
        }
        {
            this.encodeObject(object, depth);
        }
    }

    encodeNil {
        this.writeU8(0xc0);
    }

    encodeBoolean {arg object;
        this.writeU8(object.if(0xc3, 0xc2));
    }

    encodeNumber {arg object;
        if (object.isInteger and:{integerAsFloat.not} ) {
            if (object >= 0) {
                case
                { object < 0x80 } {
                    // positive fixint
                    this.writeU8(object);
                }
                { object < 0x100 } {
                    // uint8
                    this.writeU8(0xcc);
                    this.writeU8(object);
                }
                { object < 0x10000 } {
                    // uint16
                    this.writeU8(0xcd);
                    this.writeU16(object);
                }
                { object <= 0x7fffffff } {
                    // int32
                    this.writeU8(0xd2);
                    this.writeI32(object);
                }
                {
                    Error("No support for uint32/uint64 (%)".format(object)).throw;
                }
            } {
                case
                { object >= -0x20 } {
                    // negative fixint
                    this.writeU8(0xe0 | (object + 0x20));
                }
                { object >= -0x80 } {
                    // int8
                    this.writeU8(0xd0);
                    this.writeU8(object);
                }
                { object >= -0x8000 } {
                    // int16
                    this.writeU8(0xd1);
                    this.writeU16(object);
                }
                { object >= -0x80000000 } {
                    // int32
                    this.writeU8(0xd2);
                    this.writeI32(object);
                }
                {
                    Error("No support for int64 (%)".format(object)).throw;
                }
            }
        } {
            if (forceFloat32) {
                // f32
                this.writeU8(0xca);
                this.writeF32(object);
            } {
                // f64
                this.writeU8(0xcb);
                this.writeF64(object);
            }
        }
    }

    encodeString {arg object;
        var length = object.size;
        case
        { length < 32 } {
            // fixstr
            this.writeU8(0xa0 | length);
            this.writeString(object);
        }
        { length < 0x100 } {
            // str 8
            this.writeU8(0xd9);
            this.writeU8(length);
            this.writeString(object);
        }
        { length < 0x10000 } {
            // str 16
            this.writeU8(0xda);
            this.writeU16(length);
            this.writeString(object);
        }
        { length < MessagePack.maxSize } {
            // str 32
            this.writeU8(0xdb);
            this.writeI32(length);
            this.writeString(object);
        }
    }

    encodeObject {arg object, depth;
        case
        { object.isKindOf(Dictionary) } {
            this.encodeMap(object, depth);
        }
        { object.isKindOf(Array) } {
            this.encodeArray(object, depth);
        }
        {
            Error("Could not serialize object type: %".format(object.class)).throw;
        }
    }

    encodeArray {arg object, depth;
        var size = object.size;
        case
        { size < 0x10 } {
            // fixarray
            this.writeU8(0x90 | size);
        }
        { size < 0x10000 } {
            // array 16
            this.writeU8(0xdc);
            this.writeU16(size);
        }
        { size < MessagePack.maxSize } {
            // array 32
            this.writeU8(0xdd);
            this.writeI32(size);
        }
        {
            Error("Array overflow").throw;
        };
        object.do {arg v;
            this.prEncode(v, depth + 1);
        };
    }

    encodeMap {arg object, depth;
        var keys = object.keys.as(Array).sort;
        var size = keys.size;
        case
        { size < 0x10 } {
            // fixmap
            this.writeU8(0x80 | size);
        }
        { size < 0x10000 } {
            // map 16
            this.writeU8(0xde);
            this.writeU16(size);
        }
        { size < MessagePack.maxSize } {
            // map 32
            this.writeU8(0xdf);
            this.writeI32(size);
        }
        {
            Error("Map overflow").throw;
        };
        object.keysValuesDo {arg k, v;
            this.encodeString(k.asString);
            this.prEncode(v, depth + 1);
        };
    }
}

MessagePackDecoder {
    var <>options;

    *new {arg options;
        ^super.newCopyArgs(options).init;
    }

    decode {

    }
}
