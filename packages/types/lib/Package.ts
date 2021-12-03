import { Diagram } from "./Diagram";
import { Element } from "./Element";
import { IObject } from "./IObject";

export interface Package extends IObject {
  stereotype?: string;
  packages: Package[];
  parent: Package;
  elements: Element[];
  diagrams: Diagram[];
}