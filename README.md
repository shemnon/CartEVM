# CartEVM

Test Script Generator for "Cartesian" testing of EVM Operations.

## Building

Install Java 12 or greater.

Install a current solidity compiler (> 0.8.5), and have it on the execution
path.

```
brew install solidity
```

Then to build an executable version

```
./gradlew installDist
```

To launch CartEVM use `./build/install/cartevm/bin/cartevm`

### General Execution Options

For tasks that produce output, the `--output-dir` will control where the output
is generated.

To filter the names of the steps use the `--steps-regexp` option. For example
`'--steps-regexp=.*_gas'`
or `'--steps-regexp=(call|delegatecall|callcode|staticcall)'`.

`--size-limit` limits the maximum binary size of generated programs.

`--gas-limit` limites the target gas consumption of the programs

`--steps` configures the number of steps to combine. These will be combined as a
cartesian product when combining more than 1 step. A value of 1 completes in a
short time, a value of 2 in a number of hours, and 3 in a number of days.

### Run locally with the embedded Besu EVM

To run the test locally with the embedded Besu EVM pass in the `--local` command
line option.

The `--repeat` option will repeat the local execution that may times before
reporting results.

The `--verbose` option will create reports flr all runs when `--repeat` is
specified, instead of just the last run.

### Generate binary smart contract files

To generate smart contract `.bin` files (hex encoded EVM bytecode) use
the `--bytecode` command line option.

If you need the bytecode in a deployable form, also pass in the `--initcode`
command line option. The same binary program will be generate but it will have
loader code prepended to the binary.

### Generate Ethereum Consensus Tests

To generate Ethereum Fillers pass in the `--filler` command line option. These
files will then be the input to
the [Ethereum Consensus Tests](https://github.com/ethereum/tests) toolchain.
