package com.hedera.cartevm;

/*-
 * ‌
 * CartEVM
 * ​
 * Copyright (C) 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

public class Step {

  //  public static final String OP_STOP = "00";
  public static final String OP_ADD = "01";
  public static final String OP_MUL = "02";
  public static final String OP_SUB = "03";
  public static final String OP_DIV = "04";
  public static final String OP_SDIV = "05";
  public static final String OP_MOD = "06";
  public static final String OP_SMOD = "07";
  public static final String OP_ADDMOD = "08";
  public static final String OP_MULMOD = "09";
  public static final String OP_EXP = "0A";
  public static final String OP_SIGNEXTEND = "0B";
  public static final String OP_LT = "10";
  public static final String OP_GT = "11";
  public static final String OP_SLT = "12";
  public static final String OP_SGT = "13";
  public static final String OP_EQ = "14";
  public static final String OP_ISZERO = "15";
  public static final String OP_AND = "16";
  public static final String OP_OR = "17";
  public static final String OP_XOR = "18";
  public static final String OP_NOT = "19";
  public static final String OP_BYTE = "1A";
  public static final String OP_SHL = "1B";
  public static final String OP_SHR = "1C";
  public static final String OP_SAR = "1D";
  public static final String OP_SHA3 = "20";
  public static final String OP_ADDRESS = "30";
  public static final String OP_BALANCE = "31";
  public static final String OP_ORIGIN = "32";
  public static final String OP_CALLER = "33";
  public static final String OP_CALLVALUE = "34";
  public static final String OP_CALLDATALOAD = "35";
  public static final String OP_CALLDATASIZE = "36";
  public static final String OP_CALLDATACOPY = "37";
  public static final String OP_CODESIZE = "38";
  public static final String OP_CODECOPY = "39";
  public static final String OP_GASPRICE = "3A";
  public static final String OP_EXTCODESIZE = "3B";
  public static final String OP_EXTCODECOPY = "3C";
  public static final String OP_RETURNDATASIZE = "3D";
  public static final String OP_RETURNDATACOPY = "3E";
  public static final String OP_EXTCODEHASH = "3F";
  public static final String OP_BLOCKHASH = "40";
  public static final String OP_COINBASE = "41";
  public static final String OP_TIMESTAMP = "42";
  public static final String OP_NUMBER = "43";
  public static final String OP_DIFFICULTY = "44";
  public static final String OP_GASLIMIT = "45";
  public static final String OP_CHAINID = "46";
  public static final String OP_SELFBALANCE = "47";
  public static final String OP_BASEFEE = "48";
  public static final String OP_POP = "50";
  public static final String OP_MLOAD = "51";
  public static final String OP_MSTORE = "52";
  public static final String OP_MSTORE8 = "53";
  public static final String OP_SLOAD = "54";
  public static final String OP_SSTORE = "55";
  public static final String OP_JUMP = "56";
  public static final String OP_JUMPI = "57";
  public static final String OP_PC = "58";
  public static final String OP_MSIZE = "59";
  public static final String OP_GAS = "5A";
  public static final String OP_JUMPDEST = "5B";
  public static final String OP_PUSH1 = "60";
  //  public static final String OP_PUSH2 = "61";
  //  public static final String OP_PUSH3 = "62";
  //  public static final String OP_PUSH4 = "63";
  //  public static final String OP_PUSH5 = "64";
  //  public static final String OP_PUSH6 = "65";
  //  public static final String OP_PUSH7 = "66";
  //  public static final String OP_PUSH8 = "67";
  //  public static final String OP_PUSH9 = "68";
  //  public static final String OP_PUSH10 = "69";
  //  public static final String OP_PUSH11 = "6A";
  //  public static final String OP_PUSH12 = "6B";
  //  public static final String OP_PUSH13 = "6C";
  //  public static final String OP_PUSH14 = "6D";
  //  public static final String OP_PUSH15 = "6E";
  //  public static final String OP_PUSH16 = "6F";
  //  public static final String OP_PUSH17 = "70";
  //  public static final String OP_PUSH18 = "71";
  //  public static final String OP_PUSH19 = "72";
  //  public static final String OP_PUSH20 = "73";
  //  public static final String OP_PUSH21 = "74";
  //  public static final String OP_PUSH22 = "75";
  //  public static final String OP_PUSH23 = "76";
  //  public static final String OP_PUSH24 = "77";
  //  public static final String OP_PUSH25 = "78";
  //  public static final String OP_PUSH26 = "79";
  //  public static final String OP_PUSH27 = "7A";
  //  public static final String OP_PUSH28 = "7B";
  //  public static final String OP_PUSH29 = "7C";
  //  public static final String OP_PUSH30 = "7D";
  //  public static final String OP_PUSH31 = "7E";
  //  public static final String OP_PUSH32 = "7F";
  public static final String OP_DUP1 = "80";
  //  public static final String OP_DUP2 = "81";
  //  public static final String OP_DUP3 = "82";
  //  public static final String OP_DUP4 = "83";
  //  public static final String OP_DUP5 = "84";
  //  public static final String OP_DUP6 = "85";
  //  public static final String OP_DUP7 = "86";
  //  public static final String OP_DUP8 = "87";
  //  public static final String OP_DUP9 = "88";
  //  public static final String OP_DUP10 = "89";
  //  public static final String OP_DUP11 = "8A";
  //  public static final String OP_DUP12 = "8B";
  //  public static final String OP_DUP13 = "8C";
  //  public static final String OP_DUP14 = "8D";
  //  public static final String OP_DUP15 = "8E";
  //  public static final String OP_DUP16 = "8F";
  public static final String OP_SWAP1 = "90";
  //  public static final String OP_SWAP2 = "91";
  //  public static final String OP_SWAP3 = "92";
  //  public static final String OP_SWAP4 = "93";
  //  public static final String OP_SWAP5 = "94";
  //  public static final String OP_SWAP6 = "95";
  //  public static final String OP_SWAP7 = "96";
  //  public static final String OP_SWAP8 = "97";
  //  public static final String OP_SWAP9 = "98";
  //  public static final String OP_SWAP10 = "99";
  //  public static final String OP_SWAP11 = "9A";
  //  public static final String OP_SWAP12 = "9B";
  //  public static final String OP_SWAP13 = "9C";
  //  public static final String OP_SWAP14 = "9D";
  //  public static final String OP_SWAP15 = "9E";
  //  public static final String OP_SWAP16 = "9F";
  public static final String OP_LOG0 = "A0";
  public static final String OP_LOG1 = "A1";
  public static final String OP_LOG2 = "A2";
  public static final String OP_LOG3 = "A3";
  public static final String OP_LOG4 = "A4";
  public static final String OP_CREATE = "F0";
  public static final String OP_CALL = "F1";
  public static final String OP_CALLCODE = "F2";
  public static final String OP_RETURN = "F3";
  public static final String OP_DELEGATECALL = "F4";
  public static final String OP_CREATE2 = "F5";
  public static final String OP_STATICCALL = "FA";
  //  public static final String OP_REVERT = "FD";
  //  public static final String OP_INVALID = "FE";
  public static final String OP_SELFDESTRUCT = "FF";

  public static final List<Step> steps = new ArrayList<>();
  static final int NUM_PUSH0 = Integer.parseInt(OP_PUSH1, 16) - 1;
  static final int NUM_DUP0 = Integer.parseInt(OP_DUP1, 16) - 1;
  static final int NUM_SWAP0 = Integer.parseInt(OP_SWAP1, 16) - 1;

  /*
   * Memory Reservations
   * start address and the next 32 bytes
   *
   * 0x20   - SHA3
   * 0x40   - CALLDATACOPY
   * 0x60   - CODECOPY
   * 0x80   - EXTCODECOPY
   * 0xA0   - RETURNDATACOPY
   * 0xC0   - MLOAD
   * 0xE0   - MSTORE
   * 0x0100 - MSTORE8
   * 0x0120 - LOG0
   * 0x0140 - LOG1
   * 0x0160 - LOG2
   * 0x0180 - LOG3
   * 0x01A0 - LOG4
   * 0x01C0 - CREATE
   * 0x01E0 - CALL args
   * 0x0200 - CALL return
   * 0x0220 - CALLCODE args
   * 0x0240 - CALLCODE return
   * skip   - RETURN
   * 0x0260 - DELEGATECALL args
   * 0x0280 - DELEGATECALL return
   * 0x02A0 - CREATE2
   * 0x02C0 - STATICCALL args
   * 0x02C0 - STATICCALL return
   * 0x0300 - SELFDESTRUCT contract
   * 0x0320 - SELFDESTRUCT args
   * 0x0340 - SELFDESTRUCT return
   * 0x0360 - REVERT args
   * 0x0380 - REVERT return
   */

  static {
    String pushData = "0102030405060708091011121314151617181920212223242526272829303132";

    steps.add(new Step("nothing", "", "", "", 0));

    steps.add(new Step("add_small", push("02", "02"), OP_POP, OP_ADD, 11));
    steps.add(new Step("mul_small", push("02", "02"), OP_POP, OP_MUL, 13));
    steps.add(new Step("sub_small", push("02", "02"), OP_POP, OP_SUB, 11));
    steps.add(new Step("div_small", push("02", "02"), OP_POP, OP_DIV, 13));
    steps.add(new Step("sdiv_small", push("02", "02"), OP_POP, OP_SDIV, 13));
    steps.add(new Step("mod_small", push("02", "02"), OP_POP, OP_MOD, 13));
    steps.add(new Step("smod_small", push("02", "02"), OP_POP, OP_SMOD, 13));
    steps.add(new Step("addmod_small", push("02", "02", "02"), OP_POP, OP_ADDMOD, 19));
    steps.add(new Step("mulmod_small", push("02", "02", "02"), OP_POP, OP_MULMOD, 19));
    steps.add(new Step("exp_small", push("02", "02"), OP_POP, OP_EXP, 68));
    steps.add(new Step("signextend_small", push("f8", "00"), OP_POP, OP_SIGNEXTEND, 13));
    steps.add(new Step("lt", push("02", "01"), OP_POP, OP_LT, 11));
    steps.add(new Step("gt", push("02", "01"), OP_POP, OP_GT, 11));
    steps.add(new Step("slt", push("02", "01"), OP_POP, OP_SLT, 11));
    steps.add(new Step("sgt", push("02", "01"), OP_POP, OP_SGT, 11));
    steps.add(new Step("eq", push("02", "01"), OP_POP, OP_EQ, 11));
    steps.add(new Step("isZero_zero", push("00"), OP_POP, OP_ISZERO, 8));
    steps.add(new Step("isZero_small", push("01"), OP_POP, OP_ISZERO, 8));
    steps.add(
        new Step(
            "and",
            push(
                "3333333333333333333333333333333333333333333333333333333333333333",
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
            OP_POP,
            OP_AND,
            11));
    steps.add(
        new Step(
            "or",
            push(
                "3333333333333333333333333333333333333333333333333333333333333333",
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
            OP_POP,
            OP_OR,
            11));
    steps.add(
        new Step(
            "xor",
            push(
                "3333333333333333333333333333333333333333333333333333333333333333",
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
            OP_POP,
            OP_XOR,
            11));
    steps.add(
        new Step(
            "not",
            push("3333333333333333333333333333333333333333333333333333333333333333"),
            OP_POP,
            OP_NOT,
            8));
    steps.add(
        new Step(
            "byte",
            push("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "03"),
            OP_POP,
            OP_BYTE,
            11));
    steps.add(
        new Step(
            "shl",
            push("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "03"),
            OP_POP,
            OP_SHL,
            11));
    steps.add(
        new Step(
            "shr",
            push("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "03"),
            OP_POP,
            OP_SHR,
            11));
    steps.add(
        new Step(
            "sar",
            push("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "03"),
            OP_POP,
            OP_SAR,
            11));
    steps.add(
        new Step(
            "keccak",
            push("FFEEDDCCBBAA99887766554433221100FFEEDDCCBBAA99887766554433221100", "0200")
                + OP_MSTORE,
            "",
            push("20", "20"),
            OP_POP,
            OP_SHA3,
            44));

    steps.add(new Step("address", "", OP_POP, OP_ADDRESS, 4));
    steps.add(
        new Step(
            "balance",
            push("a94f5374fce5edbc8e2a8697c15331677e6ebf0b"),
            OP_POP,
            OP_BALANCE,
            405,
            2600));
    steps.add(new Step("origin", "", OP_POP, OP_ORIGIN, 4));
    steps.add(new Step("caller", "", OP_POP, OP_CALLER, 4));
    steps.add(new Step("callvalue", "", OP_POP, OP_CALLVALUE, 4));
    steps.add(new Step("calldataload", push("02"), OP_POP, OP_CALLDATALOAD, 8));
    steps.add(new Step("calldatasize", "", OP_POP, OP_CALLDATASIZE, 4));
    steps.add(new Step("calldatacopy", push("40", "04", "40"), "", OP_CALLDATACOPY, 18, 12));
    steps.add(new Step("codesize", "", OP_POP, OP_CODESIZE, 4));
    steps.add(new Step("codecopy", push("60", "04", "40"), "", OP_CODECOPY, 21, 15));
    steps.add(new Step("gasprice", "", OP_POP, OP_GASPRICE, 4));
    steps.add(
        new Step(
            "extcodesize",
            push("a94f5374fce5edbc8e2a8697c15331677e6ebf0b"),
            OP_POP,
            OP_EXTCODESIZE,
            705));
    steps.add(
        new Step(
            "extcodecopy",
            push("80", "00", "40", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b"),
            "",
            OP_EXTCODECOPY,
            724,
            2603));
    steps.add(
        new Step(
            "returndatasize",
            push("20", "40", "00", "00", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b")
                + OP_GAS
                + OP_STATICCALL,
            "",
            "",
            OP_POP,
            OP_RETURNDATASIZE,
            4,
            743));
    steps.add(
        new Step(
            "returndatacopy",
            push("20", "A0", "00", "00", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b")
                + OP_GAS
                + OP_STATICCALL,
            "",
            push("20", "00", "A0"),
            "",
            OP_RETURNDATACOPY,
            15,
            743));
    steps.add(
        new Step(
            "extcodehash",
            push("a94f5374fce5edbc8e2a8697c15331677e6ebf0b"),
            OP_POP,
            OP_EXTCODEHASH,
            405,
            2600));
    steps.add(new Step("blockhash", push("1000"), OP_POP, OP_BLOCKHASH, 25));
    steps.add(new Step("coinbase", "", OP_POP, OP_COINBASE, 4));
    steps.add(new Step("timestamp", "", OP_POP, OP_TIMESTAMP, 4));
    steps.add(new Step("number", "", OP_POP, OP_NUMBER, 4));
    steps.add(new Step("difficulty", "", OP_POP, OP_DIFFICULTY, 4));
    steps.add(new Step("gaslimit", "", OP_POP, OP_GASLIMIT, 4));
    // not in petersburg
    //    steps.add(new Step("chainid", "", OP_POP, OP_CHAINID, 4));
    //    steps.add(new Step("selfbalance", "", OP_POP, OP_SELFBALANCE, 7));
    //    steps.add(new Step("basefee", "", OP_POP, OP_BASEFEE, 4));
    steps.add(new Step("pop", push("42"), "", OP_POP, 5));
    steps.add(
        new Step("mload", push("43", "a0") + OP_MSTORE, "", push("a0"), OP_POP, OP_MLOAD, 8, 9));
    steps.add(new Step("mstore", push("8765", "c0"), "", OP_MSTORE, 9));
    steps.add(new Step("mstore8", push("a9", "e0"), "", OP_MSTORE8, 9));
    steps.add(new Step("sload", push("54"), OP_POP, OP_SLOAD, 205));
    steps.add(new Step("sstore", push("55", "55"), "", OP_SSTORE, 5006, 15000));
    steps.add(new Step("jump", OP_PC + push("05") + OP_ADD, OP_JUMPDEST, OP_JUMP, 17));
    steps.add(
        new Step("jumpi", push("01") + OP_PC + push("05") + OP_ADD, OP_JUMPDEST, OP_JUMPI, 22));
    steps.add(
        new Step("jumpi2", push("00") + OP_PC + push("05") + OP_ADD, OP_JUMPDEST, OP_JUMPI, 22));
    steps.add(new Step("pc", "", OP_POP, OP_PC, 4));
    steps.add(
        new Step("msize", push("11223344", "0100") + OP_MSTORE, "", "", OP_POP, OP_MSIZE, 5, 9));
    steps.add(new Step("gas", "", OP_POP, OP_GAS, 4));
    steps.add(new Step("jumpdest", "", "", OP_JUMPDEST, 1));

    for (int i = 1; i <= 32; i++) {
      steps.add(new Step("push" + i, "", OP_POP, push(pushData.substring(0, i * 2)), 5));
    }

    for (int i = 1; i <= 16; i++) {
      StringBuilder setupPusher = new StringBuilder();
      StringBuilder cleanupPopper = new StringBuilder();

      for (int j = 1; j <= i; j++) {
        setupPusher.append(push("1" + Integer.toHexString(j - 1)));
        cleanupPopper.append(OP_POP);
        // dup and swap
      }
      steps.add(
          new Step(
              "dup" + i,
              setupPusher.toString(),
              cleanupPopper.toString(),
              "",
              OP_POP,
              Integer.toHexString(NUM_DUP0 + i),
              5,
              6 * i));
      steps.add(
          new Step(
              "swap" + i,
              push("09") + setupPusher,
              OP_POP + cleanupPopper,
              "",
              Integer.toHexString(NUM_SWAP0 + i),
              Integer.toHexString(NUM_SWAP0 + i),
              6,
              6 * i));
    }

    steps.add(
        new Step(
            "log0", push(pushData, "0120") + OP_MSTORE, "", push("20", "0120"), "", OP_LOG0, 697));
    steps.add(
        new Step(
            "log1",
            push(pushData, "0140") + OP_MSTORE,
            "",
            push(pushData, "20", "0140"),
            "",
            OP_LOG1,
            1075));
    steps.add(
        new Step(
            "log2",
            push(pushData, "0160") + OP_MSTORE,
            "",
            push(pushData, pushData, "20", "0160"),
            "",
            OP_LOG2,
            1453));
    steps.add(
        new Step(
            "log3",
            push(pushData, "0180") + OP_MSTORE,
            "",
            push(pushData, pushData, pushData, "20", "0180"),
            "",
            OP_LOG3,
            1831));
    steps.add(
        new Step(
            "log4",
            push(pushData, "01A0") + OP_MSTORE,
            "",
            push(pushData, pushData, pushData, pushData, "20", "01A0"),
            "",
            OP_LOG4,
            2209));

    steps.add(
        new Step(
            "create",
            push(OP_CALLER + OP_SELFDESTRUCT, "01c0") + OP_MSTORE,
            "",
            push("01C0", "02", "00"),
            OP_POP,
            OP_CREATE,
            32013));
    steps.add(
        new Step(
            "call",
            push("1337", "0200") + OP_MSTORE,
            "",
            push("20", "01E0", "20", "0200", "00", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b")
                + OP_GAS,
            OP_POP,
            OP_CALL,
            745));
    steps.add(
        new Step(
            "callcode",
            push("6a51", "0240") + OP_MSTORE,
            "",
            push("20", "0220", "20", "0240", "00", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b")
                + OP_GAS,
            OP_POP,
            OP_CALLCODE,
            745));
    //     skip RETURN
    steps.add(
        new Step(
            "delegatecall",
            push("E11E", "0280") + OP_MSTORE,
            "",
            push("20", "0260", "00", "0280", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b") + OP_GAS,
            OP_POP,
            OP_DELEGATECALL,
            745));
    steps.add(
        new Step(
            "create2",
            push(OP_CALLER + OP_SELFDESTRUCT, "02A0") + OP_MSTORE,
            "",
            push("5a", "02A0", "02", "00"),
            OP_POP,
            OP_CREATE2,
            32022));
    steps.add(
        new Step(
            "staticcall",
            push("C0DE", "02E0") + OP_MSTORE,
            "",
            push("20", "02C0", "20", "02E0", "a94f5374fce5edbc8e2a8697c15331677e6ebf0b") + OP_GAS,
            OP_POP,
            OP_STATICCALL,
            745));
    steps.add(
        new Step(
            "revert",
            push("1337", "0360") + OP_MSTORE,
            "",
            push(
                "20",
                "0380",
                "20",
                "0360",
                "00",
                "7265766572742052455645525420726576657274",
                "0400"),
            OP_POP,
            OP_CALL,
            1746));
    steps.add(
        new Step(
            "selfdestruct",
            push(
                    "20",
                    "0340",
                    "20",
                    "0320",
                    "00",
                    push("02")
                        + OP_DUP1
                        + push("0C", "00")
                        + OP_CODECOPY
                        + push("00")
                        + OP_RETURN
                        + "FE"
                        + OP_CALLER
                        + OP_SELFDESTRUCT,
                    "0300")
                + OP_MSTORE
                + push("0E", "0312", "00")
                + OP_CREATE
                + OP_GAS,
            OP_POP,
            OP_CALL,
            38129));

    steps.add(new Step("extcodesize_gas", OP_GAS, OP_POP, OP_EXTCODESIZE, 705));
    steps.add(new Step("extcodehash_gas", OP_GAS, OP_POP, OP_EXTCODEHASH, 405));
    steps.add(
        new Step(
            "extcodecopy_gas", push("40", "04", "20") + OP_GAS, "", OP_EXTCODECOPY, 724, 2603));
    steps.add(new Step("balance_gas", OP_GAS, OP_POP, OP_BALANCE, 405));
    steps.add(new Step("sload_gas", OP_GAS, OP_POP, OP_SLOAD, 205));
    steps.add(new Step("sstore_gas", OP_GAS + OP_GAS, "", OP_SSTORE, 22104));
  }

  private final String name;
  private final String globalSetupCode;
  private final String globalCleanupCode;
  private final String localSetupCode;
  private final String localCleanupCode;
  private final String executionCode;
  private final int gasCost;
  private final int gasOverhead;

  public Step(
      String name,
      String localSetupCode,
      String localCleanupCode,
      String executionCode,
      int gasCost) {
    this(name, "", "", localSetupCode, localCleanupCode, executionCode, gasCost, 0);
  }

  public Step(
      String name,
      String localSetupCode,
      String localCleanupCode,
      String executionCode,
      int gasCost,
      int gasOverhead) {
    this(name, "", "", localSetupCode, localCleanupCode, executionCode, gasCost, gasOverhead);
  }

  public Step(
      String name,
      String globalSetupCode,
      String globalCleanupCode,
      String localSetupCode,
      String localCleanupCode,
      String executionCode,
      int gasCost) {
    this(
        name,
        globalSetupCode,
        globalCleanupCode,
        localSetupCode,
        localCleanupCode,
        executionCode,
        gasCost,
        0);
  }

  public Step(
      String name,
      String globalSetupCode,
      String globalCleanupCode,
      String localSetupCode,
      String localCleanupCode,
      String executionCode,
      int gasCost,
      int gasOverhead) {
    this.name = name;
    this.globalSetupCode = globalSetupCode;
    this.globalCleanupCode = globalCleanupCode;
    this.localSetupCode = localSetupCode;
    this.localCleanupCode = localCleanupCode;
    this.executionCode = executionCode;
    this.gasCost = gasCost;
    this.gasOverhead = gasOverhead;
  }

  static String push(String... values) {
    StringBuilder sb = new StringBuilder();

    for (var value : values) {
      checkNotNull(value);
      checkState(value.length() > 0, "Value must be non-empty");
      checkState(value.length() <= 64, "Value must be 32 bytes or less");
      checkState(value.length() % 2 == 0, "Value must be two-nybble bytes");

      sb.append(Integer.toHexString(NUM_PUSH0 + value.length() / 2));
      sb.append(value);
    }
    return sb.toString();
  }

  public String getName() {
    return name;
  }

  public String getGlobalSetupCode() {
    return globalSetupCode;
  }

  public String getGlobalCleanupCode() {
    return globalCleanupCode;
  }

  public String getLocalSetupCode() {
    return localSetupCode;
  }

  public String getLocalCleanupCode() {
    return localCleanupCode;
  }

  public String getExecutionCode() {
    return executionCode;
  }

  public int getGasCost() {
    return gasCost;
  }

  public int getGasOverhead() {
    return gasOverhead;
  }
}
