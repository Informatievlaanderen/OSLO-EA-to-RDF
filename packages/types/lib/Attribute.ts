import { Element } from "./Element";
import { IObject } from "./IObject";

export interface Attribute extends IObject {
  element: Element;
  type: string;
  lowerBound: string;
  upperBound: string;
}