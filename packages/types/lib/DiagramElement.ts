import { Diagram } from "./Diagram";
import { DiagramConnector } from "./DiagramConnector";
import { Element } from "./Element";

export interface DiagramElement {
  referencedElement: Element;
  diagram: Diagram;
  diagramConnectors: DiagramConnector[];
}