/*
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_BASE_COLLECTIONS_ICE
#define OMERO_BASE_COLLECTIONS_ICE

#include <omero/RTypes.ice>
#include <omero/model/NamedValue.ice>
#include <Ice/BuiltinSequences.ice>

/*
 * Defines various sequences and dictionaries used throughout
 * the OMERO API. Defining all of these in one central location
 * increases reuse and keeps the library sizes as small as possible.
 *
 * Some collections cannot be defined here since some types are not
 * yet defined.
 */
module omero {

    module api {

        //
        // Primitive Lists
        //

        ["java:type:java.util.ArrayList<String>:java.util.List<String>"]
            sequence<string> StringSet;

        ["java:type:java.util.ArrayList<Long>:java.util.List<Long>"]
            sequence<long> LongList;

        ["java:type:java.util.ArrayList<Integer>:java.util.List<Integer>"]
            sequence<int> IntegerList;

        //
        // Object lists
        //
        ["java:type:java.util.ArrayList<omero.model.NamedValue>:java.util.List<omero.model.NamedValue>"]
        sequence<omero::model::NamedValue> NamedValueList;

        // Arrays

        sequence<bool> BoolArray;
        sequence<byte> ByteArray;
        sequence<short> ShortArray;
        sequence<int> IntegerArray;
        sequence<long> LongArray;
        sequence<float> FloatArray;
        sequence<double> DoubleArray;
        sequence<string> StringArray;
        sequence<ByteArray> ByteArrayArray;
        sequence<ShortArray> ShortArrayArray;
        sequence<IntegerArray> IntegerArrayArray;
        sequence<IntegerArrayArray> IntegerArrayArrayArray;
        sequence<LongArray> LongArrayArray;
        sequence<FloatArray> FloatArrayArray;
        sequence<FloatArrayArray> FloatArrayArrayArray;
        sequence<DoubleArray> DoubleArrayArray;
        sequence<DoubleArrayArray> DoubleArrayArrayArray;
        sequence<StringArray> StringArrayArray;
        sequence<RTypeDict> RTypeDictArray;

        // Dictionaries

        dictionary<long,   string>                     LongStringMap;
        dictionary<long,   int>                        LongIntMap;
        dictionary<long,   ByteArray>                  LongByteArrayMap;
        dictionary<int,    string>                     IntStringMap;
        dictionary<int,    IntegerArray>               IntegerIntegerArrayMap;
        dictionary<int,    DoubleArray>                IntegerDoubleArrayMap;
        dictionary<string, omero::RType>               StringRTypeMap;
        dictionary<string, string>                     StringStringMap;
        dictionary<string, omero::RString>             StringRStringMap;
        dictionary<string, StringArray>                StringStringArrayMap;
        dictionary<string, long>                       StringLongMap;
        dictionary<string, int>                        StringIntMap;

        // if using to store owner and group ID, use first=owner, second=group
        struct LongPair {
          long first;
          long second;
        };

        dictionary<LongPair, long>                     LongPairLongMap;
        dictionary<LongPair, int>                      LongPairIntMap;
        dictionary<LongPair, StringLongMap>            LongPairToStringLongMap;
        dictionary<LongPair, StringIntMap>             LongPairToStringIntMap;

        // Multimaps (dictionaries with sequence values)

        dictionary<string, Ice::LongSeq>               IdListMap;
        dictionary<string, LongList>                   StringLongListMap;
        dictionary<bool,   LongList>                   BooleanLongListMap;
        dictionary<long,   BooleanLongListMap>         IdBooleanLongListMapMap;

    };

};

#endif
