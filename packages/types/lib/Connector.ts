import Direction from "./Direction";
import { Element } from "./Element";
import { IObject } from "./IObject";

export interface Connector extends IObject {
  direction: Direction;
  type: string;
  sourceRole: string;
  destinationRole: string;
  source: Element;
  destination: Element;
  associationClass: Element;
  sourceCardinality: string;
  destinationCardinality: string;
}