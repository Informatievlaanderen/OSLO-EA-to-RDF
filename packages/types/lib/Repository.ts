import { Diagram } from "./Diagram";
import { Element } from "./Element";
import { Package } from "./Package";

export interface Repository {
  rootPackage: Package;
  packages: Package[];
  elements: Element[];
  diagrams: Diagram[];
}