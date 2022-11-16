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
        { token >= 0xc4 and:{ token < 0xc7 }} {
            ^this.decodeBin(data);
        }
        { this.isNumber(token) } {
            ^this.decodeNumber(data);
        }
        { token >= 0xd4 and:{ token < 0xd9 }} {
            ^this.decodeExt(data);
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
            };
        }
    }

    decodeBin {arg data;
        var token = this.readU8(data);
        var size, bytes, array;
        case
        { token == 0xc4 } {
            size = this.readU8(data);
        }
        { token == 0xc5 } {
            size = this.readInt16(data);
        }
        { token == 0xc6 } {
            size = this.readInt32(data);
        };
        bytes = this.read(data, size);
        array = Int8Array.new;
        array = array.addAll(bytes);
        ^array;
    }

    decodeExt {arg data;
        var token = this.readU8(data);
        var type, size, bytes = Int8Array.new;
        var ext;
        case
        { token >= 0xd4 and:{ token < 0xd9 }} {
            // fixext 1-16
            type = this.readU8(data);
            size = token - 0xd3;
            size.do {arg i;
                bytes = bytes.add(this.readU8(data));
            };
        };
        ext = MessagePack.extensions[type];
        if (ext.notNil) {
            ^ext.prDecode(bytes, this);
        };
    }

    decodeString {arg data;
        var token = this.readU8(data);
        var bytes, length;
        case
        { token >= 0xa0 and:{ token < 0xc0 }} {
            length = token - 0xa0;
        }
        { token == 0xd9 } {
            length = this.readU8(data);
        }
        { token == 0xda } {
            length = this.readInt16(data);
        }
        { token == 0xdb } {
            length = this.readInt32(data);
        };
        bytes = this.read(data, length);
        ^bytes.collect(_.asAscii).join;
    }

    decodeObject {arg data, depth;
        var token = data[0];
        case
        { token >= 0x80 and:{ token < 0x90
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
        num.do {
            key = this.decodeString(data).asSymbol;
            value = this.prDecode(data, depth + 1);
            object.put(key, value);
        }
        ^object;
    }
}
