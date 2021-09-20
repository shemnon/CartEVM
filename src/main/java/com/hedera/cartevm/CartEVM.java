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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    description = "Cartesian EVM test Generator.",
    abbreviateSynopsis = true,
    name = "cartevm",
    mixinStandardHelpOptions = true,
    sortOptions = false,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    footer = "Hedera Hasgraph Services is licensed under the Apache License 2.0")
public class CartEVM implements Runnable {

  @CommandLine.Option(
      names = {"--repeat"},
      paramLabel = "int",
      description = "Number of times to repeat the whole benchmark")
  private final Integer repeat = 1;

  @CommandLine.Option(
      names = {"--verbose"},
      paramLabel = "boolean",
      description = "Report on all runs, not just the last one")
  private final Boolean verbose = false;

  @CommandLine.Option(
      names = {"--gas-limit"},
      paramLabel = "long",
      description = "Number of loop iterations")
  private final Long gasLimit = 10_000_000L;

  @CommandLine.Option(
      names = {"--steps-regexp"},
      paramLabel = "regexp",
      description = "RegExp of the steps to run")
  private String stepsRegExp = ".*";

  @CommandLine.Option(
      names = {"--size-limit"},
      paramLabel = "long",
      description = "max number of bytes for the _internal_ contract loop")
  private final Integer sizeLimit = 5120;

  @CommandLine.Option(
      names = {"--steps"},
      paramLabel = "int",
      description = "Number of steps to combine per loop")
  private final Integer steps = 2;

  @CommandLine.Option(
      names = {"--filler"},
      description = "Generate Filler")
  private final Boolean filler = false;

  @CommandLine.Option(
      names = {"--output-dir"},
      paramLabel = "<dir>",
      description = "Directory to write Ethereum test fillers")
  private final File outDir = new File("vmCartEVM");

  @CommandLine.Option(
      names = {"--local"},
      description = "Execute in embedded EVM")
  private final Boolean local = false;

  @CommandLine.Option(
      names = {"--bytecode"},
      description = "Output the bytecode")
  private final Boolean bytecode = false;

  @CommandLine.Option(
      names = {"--initcode"},
      description = "When generating bytecode, include the initcode")
  private final Boolean initcode = false;

  public static void main(String[] args) {
    CartEVM cartevm = new CartEVM();

    CommandLine commandLine = new CommandLine(cartevm);
    commandLine.execute(args);
  }

  public void runCase(List<Step> candidates, List<Step> chosen, int moreSteps, boolean verbose)
      throws IOException {
    if (moreSteps < 1) {
      createFiller(chosen, verbose);
      createBytecode(chosen);
      runLocal(chosen, verbose);
    } else {
      for (Step step : candidates) {
        chosen.add(step);
        runCase(candidates, chosen, moreSteps - 1, verbose);
        chosen.remove(chosen.size() - 1);
      }
    }
  }

  private void createFiller(List<Step> chosen, boolean verbose) throws IOException {
    if (!filler) {
      return;
    }
    FillerGenerator fillerGenerator = new FillerGenerator(chosen, gasLimit, sizeLimit);
    fillerGenerator.createFiller(outDir);
  }

  private void createBytecode(List<Step> chosen) throws IOException {
    if (!bytecode) {
      return;
    }
    ByteCodeOutput byteCodeOutput = new ByteCodeOutput(chosen, initcode, gasLimit, sizeLimit);
    byteCodeOutput.createBytecode(outDir);
  }

  private void runLocal(List<Step> chosen, boolean verbose) {
    if (!local) {
      return;
    }
    new LocalRunner(chosen, gasLimit, sizeLimit).execute(verbose);
  }

  @Override
  public void run() {
    try {
      for (int i = repeat; i > 0; i--) {
        runCase(
            Step.steps.stream()
                .filter(s -> s.getName().matches(stepsRegExp))
                .collect(Collectors.toList()),
            new ArrayList<>(steps),
            steps,
            i == 1);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
