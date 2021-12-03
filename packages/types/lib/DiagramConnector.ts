import Direction from "./Direction";
import { DiagramElement } from "./DiagramElement";
import { Connector } from "./Connector";

export interface DiagramConnector {
  referencedConnector: Connector;
  source: DiagramElement;
  destination: DiagramElement;
  associationElement: DiagramElement;
  labelDirection: Direction;
  isHidden: boolean;
}