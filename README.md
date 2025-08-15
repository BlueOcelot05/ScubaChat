# ScubaChat (Java) — Layered Network Stack & Chat TUI


ScubaChat is a teaching‑oriented, OSI‑inspired network stack written in Java. It goes from a terminal TUI at the application layer, through a reliable transport abstraction, into a network layer with Dynamic Addressing (DA) and Distance Vector (DV) routing, down to a CSMA/CA‑style link layer that communicates with a lab server.



## Features

- **Link Layer (MAC)** — CSMA-style access using `CSMA`, `Client`, and `Receiver` to arbitrate access to the medium.
- **Network Layer** — Address encapsulation via `AddressHeader`, Dynamic Addressing (`DA_Header`, `DynamicAddressing`), and DV routing (`DV_Header`, tables in `NetworkLayer`).
- **Transport Layer** — Reliable, connection-oriented messaging abstraction.
- **Application Layer** — Terminal UI (`TUI`) to whisper to a node, broadcast, query reachability, and print a lightweight topology.
- **Packet Model & Parser** — `Packet` and `PacketParser` for byte-level (de)serialization between layers.



## Quick start

**Requirements**: Java 17+ (standard JDK). No external dependencies.

**Build** (from repo root):
```bash
# Compile all sources into ./out
javac -d out $(find . -name "*.java")
```

**Run**:
```bash
java -cp out AppLayer.TUI
```


## Configuration

The link-layer client connects to a lab server. Defaults are defined in `LinkLayer.java`:

- **SERVER_IP**: `netsys.ewi.utwente.nl`
- **SERVER_PORT**: `8954`
- **frequency**: `725`
- **token**: `java-02-FE202AF81C2245C45F`
Edit these constants as needed, then rebuild.


## Usage (TUI)

At startup, the TUI shows a simple menu. Commands found in the source include:

- `BROAD`
- `HELP`
- `QUIT`
- `RANGE`
- `TOP`
- `WHISPER`

**WHISPER** sends to a specific destination node; **BROAD** broadcasts to all reachable nodes. `TOP` prints known nodes and current route costs; `RANGE` lists reachable nodes.



## Architecture (high‑level)

- **Application** — `TUI` is the entry point and interacts with the transport interface.
- **Transport** — Segments, ACKs, and reassembles data; hands packets to the network layer.
- **Network** — `NetworkLayer` maintains a routing table, wraps/unwraps `AddressHeader`, exchanges DV updates, and cooperates with `DynamicAddressing`.
- **Link** — `LinkLayer` orchestrates `CSMA`, `Client`, and `Receiver` to frame/deframe and schedule medium access.
- **Model** — `Packet`, `PacketParser`, `LayerModel`, and `LAYER` enum define cross‑layer contracts.
- **Exceptions** — `NetworkException`, `RoutingException`, `PayloadException` provide failure signaling.


## Repository layout

**AppLayer/**
  └─ TUI.java
**LinkLayer/**
  └─ CSMA.java
  └─ Client.java
  └─ LinkLayer.java
  └─ Message.java
  └─ Receiver.java
**Model/**
  └─ LAYER.java
  └─ LayerModel.java
  └─ Packet.java
  └─ PacketParser.java
**Model.Exceptions/**
  └─ NetworkException.java
  └─ PayloadException.java
  └─ RoutingException.java
**NetworkLayer/**
  └─ AddressHeader.java
  └─ DA_Header.java
  └─ DV_Header.java
  └─ NetworkLayer.java
**NetworkLayer.DA/**
  └─ DynamicAddressing.java
  └─ Node.java



## Notes

- This project is for educational use; runtime behavior depends on the provided lab server and credentials.
- A final design report is included: `Final report resit - Java2.pdf`.


## License

MPL-2.0 recommended. If absent, add a `LICENSE` file with MPL-2.0.
