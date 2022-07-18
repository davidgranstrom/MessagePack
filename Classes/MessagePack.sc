// https://github.com/msgpack/msgpack/blob/master/spec.md

// format name     |  first byte  |  first byte (in hex)
// ----------------|--------------|--------------------
// positive fixint   0xxxxxxx       0x00 - 0x7f
// fixmap            1000xxxx       0x80 - 0x8f
// fixarray          1001xxxx       0x90 - 0x9f
// fixstr            101xxxxx       0xa0 - 0xbf
// nil               11000000       0xc0
// (never used)      11000001       0xc1
// false             11000010       0xc2
// true              11000011       0xc3
// bin 8             11000100       0xc4
// bin 16            11000101       0xc5
// bin 32            11000110       0xc6
// ext 8             11000111       0xc7
// ext 16            11001000       0xc8
// ext 32            11001001       0xc9
// float 32          11001010       0xca
// float 64          11001011       0xcb
// uint 8            11001100       0xcc
// uint 16           11001101       0xcd
// uint 32           11001110       0xce
// uint 64           11001111       0xcf
// int 8             11010000       0xd0
// int 16            11010001       0xd1
// int 32            11010010       0xd2
// int 64            11010011       0xd3
// fixext 1          11010100       0xd4
// fixext 2          11010101       0xd5
// fixext 4          11010110       0xd6
// fixext 8          11010111       0xd7
// fixext 16         11011000       0xd8
// str 8             11011001       0xd9
// str 16            11011010       0xda
// str 32            11011011       0xdb
// array 16          11011100       0xdc
// array 32          11011101       0xdd
// map 16            11011110       0xde
// map 32            11011111       0xdf
// negative fixint   111xxxxx       0xe0 - 0xff

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
        initialSize = options.initialSize ? MessagePack.defaultInitialSize;
        maxDepth = options[\maxDepth] ? MessagePack.defaultMaxDepth;
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
            Error("Too many nested levels: %".format(depth)).throw;
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
    var maxDepth;
    var integerAsFloat, mapAsEvent;

    *new {arg options;
        ^super.newCopyArgs(options).init;
    }

    init {
        options = options ? ();
        maxDepth = options[\maxDepth] ? MessagePack.defaultMaxDepth;
        integerAsFloat = options.integerAsFloat ? false;
        mapAsEvent = options.mapAsEvent ? true;
    }

    readU8 {arg data;
        ^this.read(data, 1)[0];
    }

    read {arg data, length;
        var bytes = [];
        if (data.isEmpty.not and:{data.size >= length}) {
            length.do {
                bytes = bytes.add(data.removeAt(0));
            };
        };
        ^bytes;
    }

    decode {arg data = [];
        ^this.prDecode(data.copy, 0);
    }

    isNumber {arg token;
        ^(token >= 0x0 and:{ token < 0x80
          or:{ token >= 0xca and:{ token < 0xd4
          or:{ token >= 0xe0 and:{ token < 0x100 }}}}})
    }

    isFloat {arg token;
        ^token == 0xca or:{ token == 0xcb }
    }

    prDecode {arg data, depth = 0;
        var token = data[0];
        if (depth > maxDepth) {
            Error("Too many nested levels: %".format(depth)).throw;
        };
        case
        { token == 0xc0 } {
            ^this.decodeNil(data);
        }
        { token == 0xc2 or:{ token == 0xc3 }} {
            ^this.decodeBoolean(data);
        }
        { this.isNumber(token) } {
            ^this.decodeNumber(data);
        }
        { token >= 0xa0 and:{ token < 0xc0
          or:{ token >= 0xd9 and:{ token < 0xdc }}}}
        {
            ^this.decodeString(data);
        }
        {
            ^this.decodeObject(data, depth);
        }
    }

    decodeNil {arg data;
        this.readU8(data);
        ^nil;
    }

    decodeBoolean {arg data;
        var value = this.readU8(data);
        ^value == 0xc3;
    }

    readFloat32 {arg data;
        var bytes = this.read(data, 4);
        var word = (bytes[0] << 24)
                 | (bytes[1] << 16)
                 | (bytes[2] << 8)
                 | bytes[3];
        ^Float.from32Bits(word);
    }

    readFloat64 {arg data;
        var bytes = this.read(data, 8);
        var hiWord = (bytes[0] << 24)
                   | (bytes[1] << 16)
                   | (bytes[2] << 8)
                   | bytes[3];
        var loWord = (bytes[4] << 24)
                   | (bytes[5] << 16)
                   | (bytes[6] << 8)
                   | bytes[7];
        ^Float.from64Bits(hiWord, loWord);
    }

    readInt16 {arg data;
        var bytes = this.read(data, 2);
        ^(bytes[0] << 8) | bytes[1];
    }

    readInt32 {arg data;
        var bytes = this.read(data, 4);
        ^(bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
    }

    decodeNumber {arg data;
        var bytes;
        var token = this.readU8(data);
        var isFloat = this.isFloat(token);
        if (isFloat or:{integerAsFloat}) {
            if (token == 0xca) {
                ^this.readFloat32(data);
            } {
                ^this.readFloat64(data);
            }
        } {
            case
            { token < 0x80 } {
                // fixint
                ^token;
            }
            { token == 0xcc } {
                // uint8
                ^this.readU8(data);
            }
            { token == 0xcd } {
                // uint16
                ^this.readInt16(data);
            }
            { token == 0xce } {
                // uint32
                Error("uint32 can not be represented").throw;
            }
            { token == 0xcf } {
                // uint64
                Error("uint64 can not be represented").throw;
            }
            { token == 0xd0 } {
                // int8
                ^this.readU8(data);
            }
            { token == 0xd1 } {
                // int16
                ^(this.readInt16(data) - 0x10000);
            }
            { token == 0xd2 } {
                // int32
                ^this.readInt32(data);
            }
            { token == 0xd3 } {
                // int64
                Error("int64 can not be represented").throw;
            }
            { token >= 0xe0 and:{ token < 0x100 }} {
                // negative fixint
                ^token - 0x100;
            }
            {
                Error("Could not deserialize type: %".format(token)).throw;
            }
        }
    }

    decodeString {arg data;
        var token = this.readU8(data);
        var bytes, length;
        case
        { token == 0xd9 } {
            length = this.readU8(data);
        }
        {
            length = token - 0xa0;
        };
        bytes = this.read(data, length);
        ^bytes.collect(_.asAscii).join;
    }

    decodeObject {arg data, depth;
        var token = data[0];
        case
        {   token >= 0x80 and:{ token < 0x90
            or:{ token == 0xde
            or:{ token == 0xdf }}}
        } {
            ^this.decodeMap(data, depth);
        }
        { token >= 0x90 and:{ token < 0xa0
            or:{ token == 0xdc
            or:{ token == 0xdd }}}
        } {
            ^this.decodeArray(data, depth);
        }
        {
            Error("Could not deserialize type: %".format(token)).throw;
        }
    }

    decodeMap {arg data, depth;
        var token = this.readU8(data);
        var num, object;
        var key, value;
        case
        { token >= 0x80 and:{ token < 0x90 }} {
            // fixmap
            num = token - 0x80;
        }
        { token == 0xde } {
            // map 16
            num = this.readInt16(data);
        }
        { token == 0xdf } {
            // map 32
            num = this.readInt32(data);
        };
        object = IdentityDictionary.new(num);
        forBy (0, num, 2) {arg i;
            key = this.decodeString(data).asSymbol;
            value = this.prDecode(data, depth + 1);
            object.put(key, value);
        }
        ^object;
    }

    decodeArray {arg data, depth;
        var token = this.readU8(data);
        var num, object, value;
        case
        { token >= 0x90 and:{ token < 0xa0 }} {
            // fixarray
            num = token - 0x90;
        }
        { token == 0xdc } {
            // array 16
            num = this.readInt16(data);
        }
        { token == 0xdd } {
            // array 32
            num = this.readInt32(data);
        };
        object = Array.new(num);
        num.do {
            value = this.prDecode(data, depth + 1);
            object = object.add(value);
        };
        ^object;
    }
}
