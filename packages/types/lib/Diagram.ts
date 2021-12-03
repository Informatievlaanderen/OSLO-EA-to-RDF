import { DiagramElement } from "./DiagramElement";
import { Package } from "./Package";

export interface Diagram {
  name: string;
  guid: string;
  notes: string;
  package: Package;
  diagramElements: DiagramElement[];
}