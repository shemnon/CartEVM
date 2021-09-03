package com.hedera.cartevm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ByteCodeOutput extends CodeGenerator {

  static final String loaderTemplate =
      "608060405234801561001057600080fd5b5061%04x806100206000396000f3fe";

  final boolean initCode;

  public ByteCodeOutput(
      final List<Step> steps,
      final int unrolledLoopSize,
      final int outerLoopSize,
      final boolean initCode) {
    super(steps, unrolledLoopSize, outerLoopSize, 16_000_000L);
    this.initCode = initCode;
  }

  public void createBytecode(File outDir) throws IOException {
    createBytecode(outDir, getName() + ".bin");
  }

  public void createBytecode(File outDir, String fileName) throws IOException {
    Path outputFile = outDir.toPath().resolve(fileName);
    System.out.println(outputFile);
    String yulCode = generate(yulTemplate);
    String bytecode = compileYul(yulCode);
    if (bytecode.length() > 0xffff) {
      throw new RuntimeException(
          "Resulting code of " + getName() + " is too big: " + bytecode.length() + " bytes");
    }

    Files.writeString(
        outputFile, (initCode ? loaderTemplate.formatted(bytecode.length()) : "") + bytecode);
  }
}
