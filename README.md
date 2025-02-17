# Eclipse Platform Project

* ALMOST entirely -- developed by -- volunteers

## Project Description

* == 
  * basis for the [Eclipse IDE](https://www.eclipse.org/eclipseide/) + 
  * sub-repository of the [eclipse-platform](https://github.com/eclipse-platform) organization

### [eclipse.platform](platform)
* -- provides -- images (_Example:_ splash screen)

### [eclipse.resources](resources)
* -- provides -- 
  * Java interfaces (_Example:_ `IResource`)
  * implementations of 
    * workspace,
    * folder,
    * file
    * file system abstraction

### [eclipse.runtime](runtime) 
* -- provides -- 
  * Java interfaces (_Example:_ `IJob`, `ISchedulingRule`)
  * implements scheduling of multithreaded jobs / -- exclusive access to a -- resource

### [eclipse.update](update)
* -- provides --
  * Java interfaces (_Example:_ `IPlatformConfiguration`)

## How to Contribute

* [Create Eclipse Development Environment -- for -- Eclipse Platform](
https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/releng/org.eclipse.platform.setup/PlatformConfiguration.setup&show=true)

## Documentation

* [docs](./docs)

## Contact

* [project's "dev" list](https://accounts.eclipse.org/mailing-list/platform-dev)

## License

[Eclipse Public License (EPL) 2.0](https://www.eclipse.org/legal/epl-2.0/)